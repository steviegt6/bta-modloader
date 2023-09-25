package dev.tomat.modloader.mappings;

public class MappedMethod {
    public String fromName;
    public String toName;
    public String desc;

    public MappedMethod(String fromName, String toName, String desc) {
        this.fromName = fromName;
        this.toName = toName;
        this.desc = desc;
    }
}
