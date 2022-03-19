(ns dad.schema
  (:require
   [clojure.string :as str]
   [malli.core :as m]
   [malli.util :as mu]))

(defn extract-function-input-schema
  [v]
  (let [m (meta v)]
    (some-> (get m :schema
                 (get m :malli/schema))
            (m/schema)
            (m/-function-info)
            (get :input))))

;; ===== TRANSFORMER =====

(defmulti accept
  (fn [name _schema _children _options] name)
  :default ::default)

(defmethod accept ::default [_ _ _ _] {})
(defmethod accept 'string? [_ _ _ _] {:type "String"})
(defmethod accept 'map? [_ _ _ _] {:type "Map"})
(defmethod accept :not= [_ _ children _] {:type "Not=" :value children})
(defmethod accept :cat [_ _ children _] children)
(defmethod accept :and [_ _ children _] {:type "And" :value children})
(defmethod accept :or [_ _ children _] {:type "Or" :value children})
(defmethod accept :map [_ _ children _]
  (reduce
   (fn [accm [k o s]]
     (assoc accm k {:type "MapValue"
                    :required? (not (:optional o))
                    :default-value (:default o)
                    :value s}))
   {}
   children))

(defmethod accept :vector [_ _ children _] {:type "Array" :value children})
(defmethod accept :enum [_ _ children _] {:type "Enum" :value children})

(defn- docstring-schema-walker
  [schema _ children options]
  (accept (m/type schema) schema children options))

(defn transform
  ([?schema]
   (transform ?schema nil))
  ([?schema options]
   (let [options (merge options {})]
     (m/walk ?schema docstring-schema-walker options))))

;; ===== GENERATOR =====

(defmulti gen-doc :type)
(defmethod gen-doc :default [m] (:type m))
(defmethod gen-doc "Or"
  [m]
  (str/join " or " (map gen-doc (:value m))))

(defmethod gen-doc "Not="
  [m]
  (format "(not= %s)" (pr-str (first (:value m)))))

(defmethod gen-doc "And"
  [m]
  (str/join " and " (map gen-doc (:value m))))

(defmethod gen-doc "Array"
  [m]
  (format "[%s]" (gen-doc (first (:value m)))))

(defmethod gen-doc "Enum"
  [m]
  (format "#{%s}"
          (str/join ", " (map pr-str (:value m)))))

(defmethod gen-doc "MapValue"
  [m]
  (gen-doc (:value m)))

(defn function-schema->docstring
  [?function-schema]
  (let [transformed (transform (mu/get ?function-schema 0))
        has-required? (some (fn [[_ v]] (:required? v)) transformed)
        docs (cond-> (map (fn [[k v]]
                            (str "  - "
                                 (name k)
                                 (when (:required? v) "(*)")
                                 ": " (gen-doc v)))
                          transformed)
               has-required?
               (concat ["" "  (*) REQUIRED"]))]
    (str/join "\n" docs)))
