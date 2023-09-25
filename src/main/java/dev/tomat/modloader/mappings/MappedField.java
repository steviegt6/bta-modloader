package dev.tomat.modloader.mappings;

public class MappedField {
    public String fromName;
    public String toName;
    public String desc;

    public MappedField(String fromName, String toName, String desc) {
        this.fromName = fromName;
        this.toName = toName;
        this.desc = desc;
    }
}
