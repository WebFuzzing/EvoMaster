class ofTypeOn__Type {

         var name: String?= null
         var kind: __TypeKind?=null
         var ofType: ofTypeOn__Type?=null

        override fun toString(): String {
            return "{ name: ${this.name}, kind: ${this.kind}, ofType ${this.ofType}}"

        }
    }

