package dev.tomat.modloader.mappings;

import net.fabricmc.tinyremapper.IMappingProvider;

public class AbstractMappingProvider implements IMappingProvider {
    private final MappedClass[] mappings;

    public AbstractMappingProvider(MappedClass[] mappings) {
        this.mappings = mappings;
    }

    @Override
    public void load(MappingAcceptor out) {
        for (MappedClass mappedClass : mappings) {
            out.acceptClass(mappedClass.fromName, mappedClass.toName);

            for (MappedField mappedField : mappedClass.fields) {
                out.acceptField(new Member(mappedClass.toName, mappedField.fromName, mappedField.desc), mappedField.toName);
            }

            for (MappedMethod mappedMethod : mappedClass.methods) {
                out.acceptMethod(new Member(mappedClass.toName, mappedMethod.fromName, mappedMethod.desc), mappedMethod.toName);
            }
        }
    }
}
