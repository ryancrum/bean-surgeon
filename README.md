# Bean Surgeon

A somewhat silly library to modify Java source.

Extracted from an Android string-resource extractor.

## Usage

This library is very rough and doesn't do much to cover up ANTLR-generated files yet.

Here's a little example:

```clojure
;; Make all string literals uppercase:
(defn uppercase-all-strings
  [file-path]
  (let [changeset (transform
                   file-path
                   (fn
                     [node]
                     (= "STRINGLITERAL"
                        (nth bean.surgeon.antlr.JavaParser/tokenNames
                             (.getType node))))
                   (fn [sl]
                     (.toUpperCase (.getText sl))))]
    (file-diff file-path changeset)))
```

## License

Copyright (C) 2012 Ryan Crum

Distributed under the Eclipse Public License, the same as Clojure.
