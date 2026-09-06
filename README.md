# ☕ OpenFlow JVM SDK (Java & Kotlin)

Official JVM client library for [OpenFlow](https://github.com/Lab-OpenFlow/openflow), the high-performance distributed workflow orchestrator.

Compatible with **Java 17+ (LTS), Java 21**, and **Kotlin 1.9+** (Spring Boot 3, Quarkus, Micronaut, Ktor).

---

## 📦 Installation

### Maven
```xml
<dependency>
    <groupId>io.github.lab-openflow</groupId>
    <artifactId>openflow-java-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
implementation("io.github.lab-openflow:openflow-java-sdk:0.1.0")
```

### Gradle (Groovy)
```groovy
implementation 'io.github.lab-openflow:openflow-java-sdk:0.1.0'
```

---

## 🚀 Quickstart

### ☕ Java Example

```java
import io.github.labopenflow.sdk.OpenFlowClient;
import io.github.labopenflow.sdk.OpenFlowConfig;
import io.github.labopenflow.sdk.RequestOptions;
import io.github.labopenflow.sdk.model.Execution;

import java.time.Duration;
import java.util.Map;

public class Application {
    public static void main(String[] args) {
        // Initialize client
        OpenFlowClient client = OpenFlowClient.create(
            OpenFlowConfig.builder()
                .baseUrl("http://localhost:8080")
                .apiKey("your-api-key")
                .timeout(Duration.ofSeconds(10))
                .build()
        );

        // Execute workflow with idempotency key
        Execution execution = client.execute(
            "banking-transfer-saga",
            Map.of(
                "sender_id", "acc_1001",
                "receiver_id", "acc_2002",
                "amount", 1500.00
            ),
            RequestOptions.withIdempotencyKey("tx_uuid_12345")
        );

        System.out.printf("Execution started: %s (Status: %s)%n", execution.id(), execution.status());

        // Wait for final state (COMPLETED, FAILED)
        Execution result = client.waitForCompletion(execution.id(), Duration.ofSeconds(15));
        System.out.printf("Execution finished with status: %s%n", result.status());
    }
}
```

---

### 🟣 Kotlin Example (with Coroutines)

```kotlin
import io.github.labopenflow.sdk.OpenFlowClient
import io.github.labopenflow.sdk.RequestOptions
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.time.Duration

fun main() = runBlocking {
    val client = OpenFlowClient.builder()
        .baseUrl("http://localhost:8080")
        .bearerToken("jwt-token-here")
        .build()
        .let { OpenFlowClient.create(it) }

    // Asynchronous execution via CompletableFuture -> Kotlin Coroutine
    val execution = client.executeAsync(
        "order-fulfillment",
        mapOf("orderId" to "ord_999", "amount" to 89.90),
        RequestOptions.withIdempotencyKey("idemp_abc")
    ).await()

    println("Started: ${execution.id()} - Status: ${execution.status()}")

    val completed = client.waitForCompletionAsync(execution.id(), Duration.ofSeconds(10)).await()
    println("Completed: ${completed.status()} - Output: ${completed.output()}")
}
```

---

### 🍃 Spring Boot Configuration

```java
import io.github.labopenflow.sdk.OpenFlowClient;
import io.github.labopenflow.sdk.OpenFlowConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenFlowConfiguration {

    @Bean
    public OpenFlowClient openFlowClient(
            @Value("${openflow.base-url:http://localhost:8080}") String baseUrl,
            @Value("${openflow.api-key:}") String apiKey) {
        return OpenFlowClient.create(
            OpenFlowConfig.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(15))
                .build()
        );
    }
}
```

---

## ⚡ Features Supported

| Feature | Method | Description |
|:---|:---|:---|
| **Sync Execution** | `client.execute(id, payload, opts)` | Dispatches workflow execution synchronously |
| **Async Execution** | `client.executeAsync(...)` | Returns `CompletableFuture<Execution>` |
| **Wait for Completion** | `client.waitForCompletion(id, timeout)` | Polls execution status with auto-backoff |
| **Simulation (Dry-Run)** | `client.dryRun(id, payload)` | Evaluates DAG and transformations safely |
| **Task Queue Worker** | `client.pollTask(queue, workerId, timeout)` | Long-polls activity tasks with distributed leases |
| **Task Heartbeat** | `client.heartbeatTask(taskId, workerId, token)`| Renews task lease lock |
| **Task Completion** | `client.completeTask(taskId, workerId, token, output)`| Completes activity and resumes workflow |
| **DLQ Operations** | `client.listDLQMessages(status)` / `replayDLQMessage(id)` | Inspects and replays failed dead letter events |
| **Webhooks** | `client.triggerWebhook(path, payload, opts)` | Ingress for external webhook events |
| **Approvals (HITL)** | `client.decideApproval(reqId, decision, approver, reason)` | Approves or rejects human-in-the-loop review |

---

## 🧪 Building & Testing

```bash
mvn clean test
```

## 📄 License
Released under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0).