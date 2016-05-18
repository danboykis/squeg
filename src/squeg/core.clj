(ns squeg.core
  (:require [manifold.deferred :as d]
            [squeg.helpers :refer [coerce-to-int sqs-msg->map]])
  (:import [com.amazonaws.auth BasicAWSCredentials]
           [com.amazonaws.services.sqs AmazonSQSAsyncClient AmazonSQSAsync]
           [com.amazonaws.services.sqs.model ReceiveMessageRequest GetQueueUrlRequest ReceiveMessageResult DeleteMessageRequest]
           [com.amazonaws.handlers AsyncHandler]
           [com.amazonaws.regions Region Regions]))

(defn- to-receive-msg-req [{:keys [queue-url max-number-of-messages visibility-timeout-seconds wait-time-seconds]
                            :or {max-number-of-messages (int 10) visibility-timeout-seconds (int 10) wait-time-seconds (int 1)}}]
  (assert (string? queue-url) "queue-url must be a string")
  (assert (some? queue-url) "queue-url must be provided")

  (-> (ReceiveMessageRequest. queue-url)
      (.withVisibilityTimeout (coerce-to-int 'visibility-timeout-seconds visibility-timeout-seconds))
      (.withWaitTimeSeconds (coerce-to-int 'wait-time-seconds wait-time-seconds))
      (.withMaxNumberOfMessages (coerce-to-int 'max-number-of-messages max-number-of-messages))))


(defn- create-call-back [success error]
  (reify AsyncHandler
    (onSuccess [_ request result] (success request result))
    (onError [_ e] (error e))))

(defn find-queue-url [client queue-name]
  (let [d (d/deferred)
        queue-req (GetQueueUrlRequest. queue-name)
        call-back (create-call-back #(d/success! d (.getQueueUrl %2)) #(d/error! d %))]
    (.getQueueUrlAsync client queue-req call-back)
    d))

(defn delete-message [client queue-url receipt-handle]
  (let [d (d/deferred)
        call-back (create-call-back (fn [_ _] (d/success! d {:deleted true :receipt-handle receipt-handle}))
                                    (fn [ex] (d/error! d ex)))
        delete-req (DeleteMessageRequest. queue-url receipt-handle)]
    (.deleteMessageAsync client delete-req call-back)
    d))

(defn make-async-sqs-client [{:keys [access-key secret-key region]}]
  (let [basic-aws-creds (BasicAWSCredentials. access-key secret-key)
        client (doto
                 (AmazonSQSAsyncClient. basic-aws-creds)
                 (.setRegion (Region/getRegion (Regions/fromName region))))]
    {:client client
     :delete-message (partial delete-message client)}))

(defn success-handler [f]
  (fn [^ReceiveMessageRequest request ^ReceiveMessageResult result]
    (let [rs (into [] (map sqs-msg->map) (.getMessages result))]
      (doseq [r rs] (f r)))))

;TODO  --  Decide if throwing RejectedExecutionException makes sense when a client is shutdown
(defn make-receiver [^AmazonSQSAsync sqs-async-client
                     {:keys [queue-name] :as request-settings}
                     {:keys [success error]}]

  (let [queue-url @(find-queue-url sqs-async-client queue-name)
        ^ReceiveMessageRequest rmr (to-receive-msg-req (assoc request-settings :queue-url queue-url))
        call-back (create-call-back success error)]
    (fn []
      ;(when-not (-> sqs-async-client (.getExecutorService) (.isShutdown))
      (.receiveMessageAsync sqs-async-client rmr call-back))))
