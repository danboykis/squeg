(defproject squeg "0.1.0-SNAPSHOT"
  :description "Get data from SQS"
  :url "http://github.com/danboykis/squeg"
  :license {:name "Unlicense"
            :url "http://unlicense.org/"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [manifold "0.1.4"]
                 [com.amazonaws/aws-java-sdk-sqs "1.10.74"]]
  :profiles {:dev {;:source-paths ["dev" "test/resources"]
                   ;:repl-options {:init-ns dev}
                   :dependencies [[cheshire "5.6.1"]]}})
