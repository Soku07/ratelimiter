import yaml

services = ["payments", "inventory", "auth", "profile", "orders", "analytics", "search"]
actions = ["v1/process", "v2/validate", "internal/sync", "public/fetch", "secure/update"]
resources = ["batch", "single", "detail", "metadata", "status"]

rules = []
for i in range(1, 501):
    path = f"/api/{services[i%7]}/{actions[i%5]}/{resources[i%5]}/{i}"
    strategy = "AUTH_TOKEN" if i % 2 == 0 else "IP_ADDRESS"
    limit = (i % 10) + 1 # Limits between 1 and 10
    
    rules.append({
        "pathPattern": path,
        "priority": 50,
        "policy": {
            "limit": limit,
            "window": "PT1M",
            "algorithmKey": "TOKEN_BUCKET",
            "identityStrategy": strategy
        }
    })

# Add the Global Catch-all at the end
rules.append({
    "pathPattern": "/**",
    "priority": 1,
    "policy": {"limit": 100, "window": "PT1M", "algorithmKey": "TOKEN_BUCKET", "identityStrategy": "IP_ADDRESS"}
})

config = {"clients": [{"clientId": "uber-main-gateway", "rules": rules}]}

with open('D:/SpringProjects/ratelimiter/src/main/resources/rules.yml', 'w') as f:
    yaml.dump(config, f, sort_keys=False)