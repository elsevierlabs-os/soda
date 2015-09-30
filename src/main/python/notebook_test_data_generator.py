# -*- coding: utf-8 -*-
import wikipedia

TITLES = [
    "diabetes_mellitus",
    "sildenafil",
    "breast_cancer",
    "heart_failure",
    "atorvastatin"
]

fout = open("../../../src/test/resources/example.csv", 'wb')
for title in TITLES:
    print("Downloading %s..." % (title))
    p = wikipedia.page(title)
    content = p.content.encode("ascii", "ignore").replace("\n", " ")
    fout.write("%s\t%s\t%s\n" % (p.title, p.url, content))
fout.close()
