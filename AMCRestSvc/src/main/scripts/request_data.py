import requests,sys

f=open(sys.argv[0])
data = f.read()
f.close()
r = requests.post("http://178.63.22.132:8080/amc/processText", data={"text": data})
with open ('output.xml', 'w') as f: f.write (r.text)
