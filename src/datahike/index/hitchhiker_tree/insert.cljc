(ns ^:no-doc datahike.index.hitchhiker-tree.insert
  (:require [hitchhiker.tree :as tree]
            [hitchhiker.tree.op :as op]))

(defn mask [new indices]
  (reduce (fn [mask pos]
            (assoc mask pos (nth new pos)))
          [nil nil nil nil]
          indices))

(defn equals-at-indices?
  "Returns true if 'k1' and 'k2' are equals at positions given in 'indices'."
  [indices k1 k2]
  (reduce (fn [_ i]
            (if (= (nth k2 i) (nth k1 i))
              true
              (reduced false)))
          true
          indices))

(defn exists-old?
  "Returns true if 'new' already exists in 'old-keys'."
  [old-keys new]
  (when (seq old-keys)
    (let [indices [0 1 2]
          mask (mask new indices)]
      (when-let [candidates (subseq old-keys >= mask)]
        (->> candidates
             (map first)
             first
             (equals-at-indices? indices new))))))

(defrecord InsertOp [key op-count]
  op/IOperation
  (-insertion-ts [_] op-count)
  (-affects-key [_] key)
  (-apply-op-to-coll [_ kvs]
    (if (exists-old? kvs key)
      kvs
      (assoc kvs key nil)))
  (-apply-op-to-tree [_ tree]
    (let [children (cond
                     (tree/data-node? tree) (:children tree)
                     :else (:children (peek (tree/lookup-path tree key))))]
      (if (exists-old? children key)
        tree
        (tree/insert tree key nil)))))

(defrecord temporal-InsertOp [key op-count]
  op/IOperation
  (-insertion-ts [_] op-count)
  (-affects-key [_] key)
  (-apply-op-to-coll [_ kvs]
    (assoc kvs key nil)
    #_(let [old-retracted  (old-retracted kvs key indices)]
      (-> (if old-retracted
            (assoc kvs old-retracted nil)
            kvs)
          (assoc key nil))))
  (-apply-op-to-tree [_ tree]
    (tree/insert tree key nil)
    #_(let [children  (cond
                      (tree/data-node? tree) (:children tree)
                      :else (:children (peek (tree/lookup-path tree key))))
          old-retracted  (old-retracted children key indices)]
      (-> (if old-retracted
            (tree/insert tree old-retracted nil)
            tree)
          (tree/insert key nil)))))


(defn new-InsertOp [key op-count]
  (InsertOp. key op-count))


(defn new-temporal-InsertOp [key op-count]
  (temporal-InsertOp. key op-count))
