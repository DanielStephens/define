(defproject define "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.9.0"]
                 [datascript/datascript "0.16.6"]
                 [thinktopic/experiment "0.9.22"]
                 [clj-fuzzy "0.4.1"]
                 [clj-stacktrace "0.2.8"]]
  :injections [(let [orig (ns-resolve (doto 'clojure.stacktrace require)
                            'print-cause-trace)
                     new (ns-resolve (doto 'clj-stacktrace.repl require)
                           'pst)]
                 (alter-var-root orig (constantly (deref new))))])



;compile "org.deeplearning4j:deeplearning4j-core:${dl4j_version}"
;compile "org.deeplearning4j:deeplearning4j-nlp:${dl4j_version}"
;compile "org.deeplearning4j:deeplearning4j-zoo:${dl4j_version}"
;compile "org.deeplearning4j:deeplearning4j-ui_${scala_version}:${dl4j_version}"
;compile "org.nd4j:nd4j-native-platform:${dl4j_version}"