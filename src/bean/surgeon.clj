(ns bean.surgeon
  (:use [tailor.diff]))

(defn get-lines
  [file-path]
  (line-seq
   (java.io.BufferedReader.
    (java.io.FileReader. file-path))))

(defn- get-tree
  [file-path]
  (let [file-stream (org.antlr.runtime.ANTLRFileStream. file-path)
        lexer (bean.surgeon.antlr.JavaLexer. file-stream)
        token-stream (org.antlr.runtime.CommonTokenStream. lexer)
        parser (bean.surgeon.antlr.JavaParser. token-stream)]
    (.getTree (.compilationUnit parser))))

(defn- get-tokens
  [tree token-predicate]
  (filter
   token-predicate
   (tree-seq
    #(not (zero? (.getChildCount %)))
    #(.getChildren %)
    tree)))

(defn get-tokens-from-java-source
  [file-path token-predicate]
  (get-tokens (get-tree file-path) token-predicate))

(defn- produce-delta [tokens lines f]
  (loop [changed-lines {}
         remaining tokens]
          (if (seq remaining)
            (let [token (first remaining)
                  start (.getCharPositionInLine token)
                  token-size (count (.getText token))
                  stop (+ start
                          token-size)
                  line-number (.getLine token)
                  charoffset (or
                              (:charoffset (get changed-lines line-number))
                              0)
                  line-text (or
                             (:modified (get changed-lines line-number))
                             (nth lines (- line-number 1)))
                  beginning (.substring line-text 0 (+ charoffset start))
                  end (.substring line-text (+ charoffset stop))
                  new-value (f token)
                  modified (str beginning new-value end)]
              (recur
               (merge changed-lines {line-number
                                     {:charoffset (+ charoffset
                                                     (- (count new-value)
                                                        token-size))
                                      :modified modified}})
               (rest remaining)))
            changed-lines)))

(defn transform
  [file-path token-predicate f]
  (let [lines (get-lines file-path)
        delta (produce-delta
               (get-tokens-from-java-source file-path token-predicate)
               lines
               f)]
    (loop [changeset (create-changeset lines)
           offset 0
           remaining-line-numbers (sort (keys delta))]
      (if (seq remaining-line-numbers)
        (let [line-number (first remaining-line-numbers)
              line-change (get delta line-number)]
          (recur
           (change-line changeset
                        (:modified line-change)
                        line-number)
           (inc offset)
           (rest remaining-line-numbers)))
        changeset))))

