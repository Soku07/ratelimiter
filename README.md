Distributed Rate Limiter Architecture
This project implements a flexible, industry-standard rate limiting system designed to be storage-agnostic. The core logic remains consistent whether the system is running on a single machine or across a distributed cluster.

The Philosophy
The system is built on the principle of Separation of Concerns. Most rate limiters fail because database logic is baked directly into the algorithm. This architecture separates the What (the mathematical logic) from the How (the storage mechanism).

Key Components
The Brain (Algorithms): Contains pure mathematical logic, such as the Token Bucket algorithm. It defines token availability but remains unaware of the data's physical location.

The Muscle (Storage Providers): These implementations handle persistence. One version uses Caffeine for high-speed local memory, while the other uses Redis for distributed consistency.

The Contract (Interfaces): A shared interface called RateLimitDecision acts as a handshake. It ensures that regardless of the storage choice, the result is always consistent and predictable.

Architecture Principles
1. Strategy Pattern
A common StorageProvider interface allows for swapping between Redis and Caffeine at runtime without modifying algorithm logic. This makes the system highly testable and resilient to infrastructure changes.

2. Atomic Operations
Rate limiting is highly sensitive to race conditions.

Redis: Lua scripts ensure that checking and updating a limit happens as a single, uninterrupted command.

Caffeine: The compute method locks a specific key during calculation to prevent multiple threads from interfering with one another.

3. Type-Safe Agnosticism
Bounded Generics (<T extends RateLimitDecision>) ensure that the Storage Provider returns an object the Algorithm understands. This approach allows the Storage Provider to remain decoupled from the specific class names of the internal state.

Request Flow
Request Arrival: A user triggers an endpoint.

Context Creation: Policy details, such as "10 requests per minute," are gathered into a RateLimitContext.

The Hand-off: The Algorithm requests an atomicCompute operation from the Storage Provider.

Execution:

In the Redis path, a Lua script executes and returns a decision.

In the Caffeine path, Java math logic runs locally and returns the updated state.

Final Verdict: The system receives a RateLimitDecision and either allows the request or blocks it with a 429 Too Many Requests status.

Technical Intricacies
Custom Exception Mapping: Infrastructure errors are wrapped in a StorageException. This prevents low-level database failures from leaking into the business logic.

Immutable Models: Java Records are utilized for context and decisions, ensuring data cannot be accidentally modified during the request lifecycle.

Redis Stack Integration: Compatibility with Redis Stack allows for easy monitoring of keys and performance during high-traffic simulations.
