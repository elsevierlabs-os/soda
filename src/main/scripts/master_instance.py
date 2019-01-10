#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import argparse
import boto3
import logging
import os
import shutil
import time

logging.basicConfig(
   format='%(asctime)s,%(msecs)d %(levelname)-8s [%(filename)s:%(lineno)d] %(message)s',
   datefmt='%d-%m-%Y:%H:%M:%S',
   level=logging.INFO)
logger = logging.getLogger(__name__)


def load_config(config_path):
    config_dict = {}
    with open(config_path, "r") as f:
        for line in f:
            line = line.strip()
            if line.startswith("#") or len(line) == 0:
                continue
            key, value = line.split("=")
            if "," in value:
                config_dict[key] = value.split(",")
            else:
                config_dict[key] = value
    return config_dict


def save_config(config_dict, config_path):
    shutil.copy(config_path, config_path + ".bak")
    with open(config_path, "w") as f:
        for k, v in config_dict.items():
            if isinstance(v, list):
                f.write("{:s}={:s}\n".format(k, ",".join(v)))
            else:
                f.write("{:s}={:s}\n".format(k, v))
            

def copy_pem_file(pem_path):
    shutil.copy(pem_path, os.getcwd())
    return os.path.basename(pem_path).split(".")[0]
    

def remove_pem_file(pem_path):
    pem_name = os.path.basename(pem_path)
    os.remove(pem_name)


def verify_ami_exists(ec2_client, ami_id):
    resp = ec2_client.describe_images(
        ImageIds=[ami_id],
        Filters=[{
            "Name": "state",
            "Values": ["available"]
        }]
    )
    status = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status == 200:
        return len(resp["Images"]) == 1
    else:
        raise Exception(resp)
        

def verify_instance_exists(ec2_client, instance_id, is_running):
    filter_value = "running" if is_running else "stopped"
    resp = ec2_client.describe_instances(
        InstanceIds=[instance_id],
        Filters=[{
            "Name": "instance-state-name",
            "Values": [filter_value]
        }]
    )
    status = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status == 200:
        reservations = resp["Reservations"]
        if len(reservations) > 0:
            instances = reservations[0]["Instances"]
            return len(instances) > 0
        else:
            return False
    else:
        raise Exception(resp)


def start_instance(ec2_client, cfg):
    ec2_client.start_instances(InstanceIds=[cfg["INSTANCE_ID"]])


def run_instance(ec2_client, cfg, key_name):
    resp = ec2_client.run_instances(
        ImageId=cfg["IMAGE_ID"],
        MinCount=1,
        MaxCount=1,
        InstanceType=cfg["INSTANCE_TYPE"],
        KeyName=key_name,
        SubnetId=cfg["SUBNET_ID"],
        SecurityGroupIds=cfg["SECURITY_GROUP_IDS"],
        PrivateIpAddress=cfg["PRIVATE_IP_ADDRESS"],
        TagSpecifications=[{
            "ResourceType": "instance",
            "Tags": [
                { "Key": "Name", "Value": cfg["NAME_TAG"] },
                { "Key": "Owner", "Value": cfg["OWNER_TAG"] }
            ]    
        }])
    return resp["Instances"][0]["InstanceId"]


def stop_instance(ec2_client, cfg):
    ec2_client.stop_instances(InstanceIds=[cfg["INSTANCE_ID"]])


def terminate_instance(ec2_client, cfg):
    ec2_client.terminate_instances(InstanceIds=[cfg["INSTANCE_ID"]])


def create_image(ec2_client, cfg):
    image_version = int(cfg["IMAGE_VERSION"])
    image_name = "SodaSolrV2-AMI-v{:d}".format(image_version)
    resp = ec2_client.create_image(
        InstanceId=cfg["INSTANCE_ID"],
        Name=image_name,
        Description=image_name,
        NoReboot=False
    )
    status = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status == 200:
        return resp["ImageId"], image_version+1
    else:
        raise Exception(resp)


def delete_image(ec2_client, cfg):
    ec2_client.deregister_image(ImageId=cfg["IMAGE_ID"])


def get_tag_value(tags, key):
    for k in [key, key.lower(), key.title()]:
        vs = [tag["Value"] for tag in tags if tag["Key"] == k]
        if len(vs) > 0:
            return vs[0]
    return None


def load(cfg_path):
    ec2 = boto3.client("ec2")
    cfg = load_config(cfg_path)
    # verify AMI exists
    ami_exists = verify_ami_exists(ec2, cfg["IMAGE_ID"])
    if not ami_exists:
        logger.error("AMI {:s} does not exist, aborting"
                     .format(cfg["IMAGE_ID"]))
        return
    # if instance exists in the config, this means that the instance
    # hasn't been saved yet. Abort with message to save or remove 
    # INSTANCE_ID entry from configuration
    if "INSTANCE_ID" in cfg.keys():
        logger.warn("Unsaved Instance {:s} detected! Either run save first or remove INSTANCE_ID entry from configuration file"
                    .format(cfg["INSTANCE_ID"]))
        return        
    # copy PEM file to local
    key_name = copy_pem_file(cfg["KEY_FILE"])
    # create instance from AMI
    instance_id = run_instance(ec2, cfg, key_name)
    logger.info("Created new instance {:s} from AMI {:s}"
        .format(instance_id, cfg["IMAGE_ID"]))
    # remove PEM file from local
    remove_pem_file(cfg["KEY_FILE"])
    # update instance ID in config
    cfg["INSTANCE_ID"] = instance_id
    save_config(cfg, cfg_path)


def start(cfg_path):
    ec2 = boto3.client("ec2")
    cfg = load_config(cfg_path)
    # verify instance exists in config
    if not "INSTANCE_ID" in cfg.keys():
        logger.warn("No Instance found to start. Run load to create instance from AMI {:s}"
                    .format(cfg["IMAGE_ID"]))
        return
    # verify instance NOT running
    instance_exists = verify_instance_exists(ec2, cfg["INSTANCE_ID"], False)
    if not instance_exists:
        logger.warn("Instance {:s} not stopped, cannot start".format(cfg["INSTANCE_ID"]))
        return
    # start instance
    logger.info("Starting instance {:s}".format(cfg["INSTANCE_ID"]))
    start_instance(ec2, cfg)


def stop(cfg_path):
    ec2 = boto3.client("ec2")
    cfg = load_config(cfg_path)
    # verify instance exists in config
    if not "INSTANCE_ID" in cfg.keys():
        logger.warn("No Instance found to stop. Run load to create instance from AMI {:s}"
                    .format(cfg["IMAGE_ID"]))
        return
    # verify instance running
    instance_exists = verify_instance_exists(ec2, cfg["INSTANCE_ID"], True)
    if not instance_exists:
        logger.warn("Instance {:s} not running, cannot stop".format(cfg["INSTANCE_ID"]))
        return
    # stop instance
    logger.info("Stopping instance {:s}".format(cfg["INSTANCE_ID"]))
    stop_instance(ec2, cfg)


def save(cfg_path):
    ec2 = boto3.client("ec2")
    cfg = load_config(cfg_path)
    # create image to new AMI, hold on to new AMI id
    logger.info("Creating new AMI from instance {:s}..."
        .format(cfg["INSTANCE_ID"]))
    new_image_id, new_image_version = create_image(ec2, cfg)
    # wait for image to become available
    secs_waited, sleep_secs = 0, 60
    while True:
        time.sleep(sleep_secs)
        secs_waited += sleep_secs
        logger.info("Waiting {:d}s for new AMI {:s} to be built..."
                .format(secs_waited, new_image_id))
        new_image_available = verify_ami_exists(ec2, new_image_id)
        
        if new_image_available:
            break
    # terminate instance
    logger.info("Terminating instance {:s}...".format(cfg["INSTANCE_ID"]))
    terminate_instance(ec2, cfg)
    # deregister old AMI
    logger.info("Deregistering old AMI {:s}".format(cfg["IMAGE_ID"]))
    delete_image(ec2, cfg)
    # unset instance ID in config 
    del cfg["INSTANCE_ID"]
    # update AMI id AMI version
    cfg["IMAGE_ID"] = new_image_id
    cfg["IMAGE_VERSION"] = str(new_image_version)
    save_config(cfg, cfg_path)
    pass

################################## main #################################


def main():
    parser = argparse.ArgumentParser(
        description="Start or stop master SoDA instance")
    parser.add_argument("-c", "--config", help="path to config file")
    parser.add_argument("-m", "--command", help="one of load|start|stop|save")
    args = parser.parse_args()
    if args.config is None or args.command is None:
        parser.print_help()
        return

    if args.command == "load":
        load(args.config)
    elif args.command == "start":
        start(args.config)
    elif args.command == "stop":
        stop(args.config)
    elif args.command == "save":
        save(args.config)
    else:
        logger.error("Unknown command {:s}".format(args.command))
        parser.print_help()


if __name__ == "__main__":
    main()

