(ns define.modelling.model
  (:require [define.modelling.transformation :as trans]
            [clojure.java.io :as io]
            [cortex.experiment.train :as train]
            [cortex.nn.execute :as execute]
            [cortex.nn.layers :as layers]
            [cortex.nn.network :as network]
            [cortex.util :as util])
  (:import (java.io File)))

(defn- nippy-file [name]
  (str name ".nippy"))

(defn- model-file-exists? [name]
  (.exists (io/as-file (nippy-file name))))

(defn- delete-model [name]
  (io/delete-file (nippy-file name) true))

(defn- read-model [name]
  (util/read-nippy-file (nippy-file name)))

(defn- create-model [{:keys [inputs outputs]}]
  (let [i inputs
        o outputs
        a (* 3 (Math/ceil (/ (+ i o) 2)))]
    (network/linear-network
     [(layers/input i 1 1 :id :input)
      (layers/linear a)
      (layers/linear o :id :output)
      ])))

(defn- resolve-model [name size]
  (if (model-file-exists? name)
    (read-model name)
    (create-model size)))

(defn- measure-data [data]
  (let [{input :input output :output} (first data)
        i (count input)
        o (count output)]
    {:inputs i :outputs o}))

(defn- do-train [name epochs train-data test-data]
  (let [size (measure-data train-data)
        network (resolve-model name size)
        batch (max 1 (int (/ (count train-data) 20)))]
    (train/train-n network train-data test-data
                   :batch-size batch
                   :network-filestem name
                   :epoch-count epochs)))

(defn- do-predict [name inputs]
  (execute/run
    (resolve-model name (measure-data [{:input inputs}]))
    inputs))

(defprotocol Network
  (reset [n] "Forgets previous learning")
  (current [n])
  (learn [n epochs train-data test-data] "Continues learning with the given data")
  (predict [n record] "Predicts and output for the given input"))

(defrecord CortexNetwork [name transformer]
  Network
  (reset [_]
    (delete-model name))
  (current [_]
    (if (model-file-exists? name)
      (read-model name)
      :no-model))
  (learn [_ epochs train-data test-data]
    (let [discrete #(map (fn [x] (trans/group->input transformer x)) %)
          d-train (discrete train-data)
          d-test (discrete test-data)]
      (do-train name epochs d-train d-test)))
  (predict [_ record]
    (let [d-record (trans/record->input transformer record)
          d-output (first (do-predict name [d-record]))]
      (trans/output->record transformer d-output)
      )))