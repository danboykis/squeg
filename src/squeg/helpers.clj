(ns squeg.helpers
  (:import [com.amazonaws.services.sqs.model Message]))

(defn coerce-to-int [n-sym n]
  (cond
    (instance? Integer n) n
    (and (number? n) (<= n Integer/MAX_VALUE) (> n 0)) (int n)
    :else (throw (AssertionError. (str "argument " n-sym " with value of " n " is not a valid int")))))

(defn sqs-msg->map [^Message m]
  (assert (some? m) "message cannot be nil")
  (let [body (.getBody m)
        md5 (.getMD5OfBody m)
        message-id (.getMessageId m)
        receipt-handle (.getReceiptHandle m)]

    {:body body :md5 md5 :message-id message-id :receipt-handle receipt-handle}))

