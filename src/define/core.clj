(ns define.core
  (:require [define.generation.group :as g]
            [define.modelling.transformation :as t]
            [define.modelling.model :as m]
            [clojure.pprint :as p]))

;Setup some data
(def data-count 10000)
(def train-data (g/generate-groups data-count))
(def test-data (g/generate-groups data-count))

;Build transformer for sample data
;This helps us turn values like :credit into something that the network can use to train or predict
(def all-data (t/groups->records (concat train-data test-data)))
(def transformer (t/transformer all-data))

;Setup our network with built in data transformer
(def network (m/->CortexNetwork "my-network" transformer))

(defn predict
  "predict an output for a given input"
  [group]
  (let [r (:input group)
        p (m/predict network r)]
    (merge {:input r :actual (:output group) :predicted p})))

(defn predict-something!
  "predict an output for a newly generated input"
  []
  (predict (first (g/generate-groups 1))))

;What am I
;Some fun with neural networks and transformations to change standard records into measurable data, and to the predict connections between input and output records
;
;
;
;Where's the data coming from
;within define.generation.group is our definition for how we create a grouping, currently the data is linked in the following way:
;  {:input [{:name :identifier, :type :string, :value A_RANDOM_UUID}
;           {:name :type, :type :enum, :options [:credit :debit], :value CREDIT_OR_DEBIT}
;           {:name :value, :type :integer, :value VALUE}
;           {:name :day, :type :integer, :value DAY}],
;   :actual [{:name :identifier, :type :string, :value A_RANDOM_UUID}
;            {:name :type, :type :enum, :options [:credit :debit], :value CREDIT_OR_DEBIT_OPPOSITE_OF_INPUT}
;            {:name :value, :type :integer, :value VALUE_THE_SAME_AS_INPUT}
;            {:name :day, :type :integer, :value DAY_ONE_LESS_THAN_INPUT_IF_CREDIT_OTHERWISE_ONE_MORE}]}
;There are only a few supported types currenty
;  :string
;  :enum
;  :integer
;
;
;
;Some code to execute
;The following has some examples of code that you can run in the REPL to see the training in action



;Check whether we have a reasonably well trained model, surprisingly with the current setup, the loss can usually get to that range of 1E-16 within about 100 epochs
(let [loss (:cv-loss (m/current network))]
  (if
    (> loss 1E-13)
    (println "The current model still has quite a high loss, more training is suggested")
    (do
      (println "Training looks successful, see what you think")
      (p/pprint (predict-something!)))))

;Reset the network
;This will delete the current .nippy file and mean that the next call to learn will generate a new model.
;You may wish to do this if you are changing the structure of the records, or you could change the name of the network at line 17 to avoid overwritting this file.
;  (m/reset network)

;Train the network
;  (m/learn network 100 train-data test-data)

;Predicting
;Predict something it might have seen before by generating another record and seeing what it expects the output to be
;  (predict-something!)

;Predict something it's never seen, we only train the model with positive number, so how well does it work out the relationship for something new, negative numbers!
;If you are worried it's cheating, you don't need to specify the expected output when predicting.
;  (predict {:input [{:name :identifier, :type :string, :value "a1a7bf77-1b57-4079-acbd-14d24091932c"}
;                    {:name :type, :type :enum, :options [:credit :debit], :value :credit}
;                    {:name :value, :type :integer, :value -100}
;                    {:name :day, :type :integer, :value -70}],
;            ;:output [{:name :identifier, :type :string, :value "40ebe333-affa-4a2f-ae24-e5d087131460"}
;            ;         {:name :type, :type :enum, :options [:credit :debit], :value :debit}
;            ;         {:name :value, :type :integer, :value 692}
;            ;         {:name :day, :type :integer, :value 1001}]
;            })
