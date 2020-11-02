class __Field {

     var name: String?=null
     var args = ArrayList<InputValue?>()
     var type : TypeRef?=null
     var isDeprecated: Boolean= true
     var deprecationReason: String?= null




    override fun toString(): String {
        return "{ name: ${this.name}, args: ${this.args}, type: ${this.type}, isDeprecated: ${this.isDeprecated}," +
                " deprecationReason: ${this.deprecationReason} }"

    }
}
