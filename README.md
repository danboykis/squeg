# squeg

Amazon SQS library for Clojure

# Description

`squeg` tries to be embrace async workflow, therefore the primary goal is to wrap [AmazonSQSAsyncClient](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/sqs/AmazonSQSAsyncClient.html)
functionality.

# Example

```clojure
(require '[squeg.core :refer [make-async-sqs-client make-receiver success-handler]])
(import 'java.util.concurrent.ArrayBlockingQueue)

;; Data goes into the queue
(def block-q (ArrayBlockingQueue. 100))

(def creds {:access-key "..."
            :secret-key "..."
            :region "us-east-1"})

(def amazon-client (make-async-sqs-client creds))

amazon-client

=> {:client #object[com.amazonaws.services.sqs.AmazonSQSAsyncClient 0x4393c928 com.amazonaws.services.sqs.AmazonSQSAsyncClient@4393c928],
    :delete-message #object[clojure.core$partial$fn__4759 0x4bb1e2c5 clojure.core$partial$fn__4759@4bb1e2c5]}

(def get-data (make-receiver (:client amazon-client) {:queue-name "your-queue"} {:success (success-handler #(.put block-q %)) :error println}))
(count block-q)
=> 0
(get-data)
(count block-q)
=> 10
```
