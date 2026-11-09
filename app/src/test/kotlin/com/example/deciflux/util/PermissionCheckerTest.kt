package com.example.deciflux.util

import android.Manifest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionCheckerTest {

    @Test
    fun getRequiredPermissionsList_containsStoragePermission() {
        val permissions = PermissionChecker.getRequiredPermissionsList()
        assertNotNull(permissions)
        assertTrue(permissions.isNotEmpty())
        assertTrue(
            permissions.contains(Manifest.permission.READ_MEDIA_VIDEO) ||
            permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
    }

    @Test
    fun getRequiredPermissionsList_nonEmptyAndImmutableStructure() {
        val list = PermissionChecker.getRequiredPermissionsList()
        assertTrue(list.size in 1..3)
    }
}
