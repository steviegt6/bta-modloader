package dev.tomat.modloader.mappings;

public class MappedClass {
    public String fromName;
    public String toName;
    public MappedField[] fields;
    public MappedMethod[] methods;

    public MappedClass(String fromName, String toName, MappedField[] fields, MappedMethod[] methods) {
        this.fromName = fromName;
        this.toName = toName;
        this.fields = fields;
        this.methods = methods;
    }
}
