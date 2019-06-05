/*
 return the value in object where the key (regex)
 matches the given patter
*/
def call(Map<String, Object> regexObj, pattern) {
    if (regexObj != null) {
        return regexObj
    }

    def key = regexObj.keySet().find { pattern ==~ it }
    return key ? regexObj[key] : null
}