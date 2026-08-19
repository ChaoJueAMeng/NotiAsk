package com.notiask.ui.backdrop

import com.notiask.data.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelMarkTest {
    @Test
    fun everyProviderHasAMark() {
        assertEquals(ProviderKind.entries.size, ModelMark.entries.size)
        ProviderKind.entries.forEach { kind ->
            assertEquals(kind, ModelMark.forProvider(kind).provider)
        }
    }
}
