import urllib.request
import urllib.error
import json
import random

email = f"test{random.randint(1000,9999)}@example.com"
url_auth = "https://sem2grp2.istic.univ-rennes1.fr/api/auth.php"

# Register
data = {
    "action": "register",
    "nameUsers": "Test User",
    "emailUser": email,
    "passwordUser": "password",
    "sexe": "Homme",
    "birthDate": "1990-01-01",
    "weight": 70.0,
    "height": 180.0
}
req = urllib.request.Request(url_auth, data=json.dumps(data).encode('utf-8'), headers={'Content-Type': 'application/json'})
try:
    with urllib.request.urlopen(req) as response:
        print("Register:", response.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("Register Error:", e.read().decode('utf-8'))

# Login
data_login = {
    "action": "login",
    "emailUser": email,
    "passwordUser": "password"
}
req = urllib.request.Request(url_auth, data=json.dumps(data_login).encode('utf-8'), headers={'Content-Type': 'application/json'})
token = None
try:
    with urllib.request.urlopen(req) as response:
        resp_text = response.read().decode('utf-8')
        print("Login:", resp_text)
        # extract json
        start = resp_text.find('{')
        end = resp_text.rfind('}')
        if start != -1 and end != -1:
            resp_json = json.loads(resp_text[start:end+1])
            token = resp_json.get('token')
except urllib.error.HTTPError as e:
    print("Login Error:", e.read().decode('utf-8'))

# Get Patient
if token:
    url_patient = "https://sem2grp2.istic.univ-rennes1.fr/api/patient.php"
    req = urllib.request.Request(url_patient, headers={'Authorization': f'Bearer {token}'})
    try:
        with urllib.request.urlopen(req) as response:
            print("Patient GET:", response.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        print("Patient GET Error:", e.read().decode('utf-8'))
