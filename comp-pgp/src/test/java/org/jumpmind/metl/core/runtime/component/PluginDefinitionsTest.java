package org.jumpmind.metl.core.runtime.component;

import static org.jumpmind.metl.core.runtime.component.PgpConfiguration.SYMMETRIC_KEY_ALGORITHM;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.plugin.XMLSetting;
import org.jumpmind.metl.core.runtime.component.PgpConfiguration.SymmetricKeyAlgorithm;
import org.junit.Test;

public class PluginDefinitionsTest extends MetlTestSupport {
    @Test
    public void symmetricKeyChoicesRestrictedToApprovedByDefault() {
        Set<String> approvedChoices = SymmetricKeyAlgorithm.allApproved().stream()
                .map(a -> a.toString()).collect(Collectors.toSet());
        XMLComponentDefinition pgpEncryptDefinition = findXMLComponentDefition("PGP Encrypt");
        XMLSetting keyAlgorithm = pgpEncryptDefinition.getSettings().getSetting().stream()
                .filter(s -> SYMMETRIC_KEY_ALGORITHM.equals(s.getId())).findFirst().orElse(null);
        Set<String> choices = new HashSet<>(keyAlgorithm.getChoices().getChoice());

        /*
         * implicitly tests the opposite as well (i.e. none of the NON-approved
         * algorithms can be chosen by default)
         */
        assertEquals(approvedChoices, choices);
    }

    @Test
    public void defaultSymmetricKeyAlgorithmIsAes128() {
        XMLComponentDefinition pgpEncryptDefinition = findXMLComponentDefition("PGP Encrypt");
        XMLSetting keyAlgorithm = pgpEncryptDefinition.getSettings().getSetting().stream()
                .filter(setting -> SYMMETRIC_KEY_ALGORITHM.equals(setting.getId())).findFirst()
                .orElse(null);

        assertEquals(SymmetricKeyAlgorithm.AES_128.toString(), keyAlgorithm.getDefaultValue());
    }
}
