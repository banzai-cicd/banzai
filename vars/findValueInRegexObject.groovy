/*
 return the value in object where the key (regex)
 matches the given targetString string
*/
def call(Map<String, Object> regexObj, String targetString) {
    if (regexObj == null) {
        return regexObj
    }

    def key = regexObj.keySet().find { targetString ==~ it }
    return key ? regexObj[key] : null
}