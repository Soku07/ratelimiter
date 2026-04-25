import random
from locust import HttpUser, task, between

SERVICES = ["payments", "inventory", "auth", "profile", "orders", "analytics", "search"]
ACTIONS = ["v1/process", "v2/validate", "internal/sync", "public/fetch", "secure/update"]
RESOURCES = ["batch", "single", "detail", "metadata", "status"]

def generate_500_endpoints():
    endpoints = []
    while len(endpoints) < 3:
        # Create varied paths like: /api/payments/v2/validate/batch/xyz-99
        path = f"/api/{random.choice(SERVICES)}/{random.choice(ACTIONS)}/{random.choice(RESOURCES)}/{random.getrandbits(16)}"
        if path not in endpoints:
            endpoints.append(path)
    return endpoints

ALL_ENDPOINTS = generate_500_endpoints()

class loadtest(HttpUser):
    wait_time = between(0.001, 0.4)
    @task
    def hit_complex_api(self):
        # Randomly select from our diverse 500 paths
        target_path = random.choice(ALL_ENDPOINTS)
        
        # Identity Headers
        headers = {
            "Authorization": f"Bearer {random.getrandbits(32)}",
            "X-Forwarded-For": f"{random.randint(1,255)}.{random.randint(1,255)}.1.1",
            # "X-Client-ID": random.choice(["uber-driver", "uber-eats", "uber-admin"])
        }

        # The 'name' parameter groups these 500 unique URLs into one report line
        with self.client.get(target_path, headers=headers, catch_response=True, name="Dynamic_Microservice_Path") as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 429:
                # Stop calling this success! 
                # By marking it as failure, the 'Failures' column will show your blocked requests.
                response.failure("Rate Limited (429)") 
            else:
                response.failure(f"System Error: {response.status_code}")