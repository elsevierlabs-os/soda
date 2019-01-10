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


def start_instances(conf_dict, num_slaves):
    # verify AMI exists
    ec2_client = boto3.client("ec2")
    # verify AMI exists
    ami_id = conf_dict["IMAGE_ID"]
    resp = ec2_client.describe_images(
        ImageIds=[ ami_id ]
    )
    status = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status != 200:
        logger.warn("Could not verify AMI exists, exiting")
        return []
    if len(resp["Images"]) == 0:
        logger.warn("AMI ({:s}) not found, exiting".format(ami_id))
    # verify instances are not already running
    if "INSTANCE_IDS" in conf_dict.keys():
        logger.warn("Instances are already running, exiting")
        instance_ids = conf_dict["INSTANCE_IDS"]
        return instance_ids
    # copy PEM file to local
    key_name = copy_pem_file(conf_dict["KEY_FILE"])
    instance_ids = []
    for i in range(num_slaves):
        # create instances separately
        instance_name = "{:s}-worker-{:d}".format(conf_dict["NAME_TAG"], i+1)
        logger.info("Creating worker instance: {:s}".format(instance_name))
        resp = ec2_client.run_instances(
            ImageId=conf_dict["IMAGE_ID"],
            MinCount=1,
            MaxCount=1,
            InstanceType=conf_dict["INSTANCE_TYPE"],
            KeyName=key_name,
            SubnetId=conf_dict["SUBNET_ID"],
            SecurityGroupIds=conf_dict["SECURITY_GROUP_IDS"],
            TagSpecifications=[
                {
                    "ResourceType": "instance",
                    "Tags": [
                        { 
                            "Key": "Name", 
                            "Value": instance_name
                        },
                        {
                            "Key": "Owner", 
                            "Value": conf_dict["OWNER_TAG"] 
                        }
                    ]    
                }
            ]
        )
        status = resp["ResponseMetadata"]["HTTPStatusCode"]
        if status != 200:
            logger.warn("Worker instance creation failed (status: {:d}), skipping"
                .format(status))
            continue
        instance_ids.append(resp["Instances"][0]["InstanceId"])
    remove_pem_file(conf_dict["KEY_FILE"])
    return instance_ids


def start_load_balancer(conf_dict, instance_ids):
    elb_name = "{:s}-alb".format(conf_dict["NAME_TAG"])
    elb_client = boto3.client("elbv2")
    # load balancer
    logger.info("Creating load balancer...")
    resp = elb_client.create_load_balancer(
        Name=elb_name,
        Subnets=conf_dict["LB_SUBNET_IDS"],
        SecurityGroups=conf_dict["LB_SECURITY_GROUP_IDS"],
        Scheme="internet-facing",
        Tags=[
            {
                "Key": "Name",
                "Value": elb_name
            },
            {
                "Key": "Owner",
                "Value": conf_dict["OWNER_TAG"]
            }
        ],
        Type="application",
        IpAddressType="ipv4"
    )
    status = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status != 200:
        logger.warn("Load balancer creation failed (status: {:d}), exiting"
                    .format(status))
        return None, None, None
    lb_arn = resp["LoadBalancers"][0]["LoadBalancerArn"]
    lb_hostname = resp["LoadBalancers"][0]["DNSName"]
    # create target group
    logger.info("Creating target group for Load balancer...")
    resp = elb_client.create_target_group(
        Name="{:s}-targets".format(conf_dict["NAME_TAG"]),
        Protocol="HTTP",
        Port=int(conf_dict["HTTP_PORT"]),
        VpcId=conf_dict["LB_VPC_ID"],
        HealthCheckProtocol="HTTP",
        HealthCheckIntervalSeconds=300,
        HealthCheckTimeoutSeconds=120,
        HealthCheckPath=conf_dict["LB_STATUS_URL"],
        TargetType="instance"
    )
    status = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status != 200:
        logger.warn("Target group creation failed (status: {:d}), exiting"
                    .format(status))
        return None, None, None
    lb_tgarn = resp["TargetGroups"][0]["TargetGroupArn"]
    # register targets
    logger.info("Registering targets for Load balancer...")
    num_tries = 0
    while num_tries < 3:
        try:
            resp = elb_client.register_targets(
                TargetGroupArn=lb_tgarn,
                Targets=[
                    {"Id": instance_id} for instance_id in instance_ids
                ]
            )
            status = resp["ResponseMetadata"]["HTTPStatusCode"]
            if status != 200:
                logger.warn("Registering targets failed (status: {:d}), exiting"
                            .format(status))
                return None, None, None
            break
        except:
            time.sleep(10)
            num_tries += 1
    # create listener
    logger.info("Creating listener for Load balancer...")
    resp = elb_client.create_listener(
        LoadBalancerArn=lb_arn,
        Protocol="HTTP",
        Port=int(conf_dict["HTTP_PORT"]),
        DefaultActions=[
            {
                "Type": "forward",
                "TargetGroupArn": lb_tgarn
            }
        ]
    )
    status = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status != 200:
        logger.warn("Creating listener failed (status: {:d}), exiting"
                    .format(status))
        return None, None, None
    logger.info("Load balancer created successfully")
    return lb_hostname, lb_arn, lb_tgarn


def stop_load_balancer(conf_dict):
    if ("ARN_TG" not in conf_dict or "ARN_LB" not in conf_dict):
        logger.warn("Cluster already stopped")
        return
    elb_client = boto3.client("elbv2")
    # delete load balancer
    logger.info("Deleting load balancer...")
    resp = elb_client.delete_load_balancer(
        LoadBalancerArn=conf_dict["ARN_LB"]        
    )
    status_dlb = resp["ResponseMetadata"]["HTTPStatusCode"]
    # delete target group
    logger.info("Deleting target group for load balancer...")
    num_tries = 0
    while num_tries < 3:
        try:
            resp = elb_client.delete_target_group(
                TargetGroupArn=conf_dict["ARN_TG"]
            )
            break
        except:
            time.sleep(10)
            num_tries += 1
    status_dtg = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status_dtg != 200 or status_dlb != 200:
        logger.warn("LB not stopped properly, exiting")
    return


def stop_instances(conf_dict):
    if "INSTANCE_IDS" not in conf_dict:
        logger.warn("No instances found to stop, exiting")
        return
    logger.info("Terminating worker instances...")
    ec2_client = boto3.client("ec2")
    resp = ec2_client.terminate_instances(
        InstanceIds=conf_dict["INSTANCE_IDS"]
    )
    status = resp["ResponseMetadata"]["HTTPStatusCode"]
    if status != 200:
        logger.warn("Worker Instances not shut down properly!")
    logger.info("Worker instances shut down")
    return


def start_cluster(config_path, num_slaves):
    conf_dict = load_config(config_path)
    instance_ids = start_instances(conf_dict, num_slaves)
    if len(instance_ids) == 0:
        logger.warn("Cluster not started, no instances available")
        return
    lb_hostname, lb_arn, lb_tgarn = start_load_balancer(conf_dict, 
                                                        instance_ids)
    # save new configurations
    conf_dict["ARN_LB"] = lb_arn
    conf_dict["ARN_TG"] = lb_tgarn
    conf_dict["INSTANCE_IDS"] = instance_ids
    save_config(conf_dict, config_path)
    logger.info("Cluster started with {:d} workers, LB hostname: {:s}"
        .format(num_slaves, lb_hostname))
    return
    

def stop_cluster(config_path):
    conf_dict = load_config(config_path)
    stop_load_balancer(conf_dict)
    stop_instances(conf_dict)
    del conf_dict["ARN_LB"]
    del conf_dict["ARN_TG"]
    del conf_dict["INSTANCE_IDS"]
    save_config(conf_dict, config_path)
    logger.info("Cluster stopped")
    return


################################## main #################################

def main():
    parser = argparse.ArgumentParser(
        description="Start or stop read-only SoDA cluster")
    parser.add_argument("-c", "--config", 
                        help="path to config file")
    parser.add_argument("-n", "--num_slaves", 
                        help="number of SoDA slaves")
    parser.add_argument("-m", "--command", 
                        help="one of start|stop")
    args = parser.parse_args()
    if args.config is None or \
            args.num_slaves is None or \
            args.command is None:
        parser.print_help()
        return

    if args.command == "start":
        start_cluster(args.config, int(args.num_slaves))
    elif args.command == "stop":
        stop_cluster(args.config)
    else:
        logger.error("Unknown command {:s}".format(args.command))
        parser.print_help()


if __name__ == "__main__":
    main()