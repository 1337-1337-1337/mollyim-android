package org.thoughtcrime.securesms.util

import org.thoughtcrime.securesms.BuildConfig

object Environment {
  const val IS_STAGING: Boolean = BuildConfig.BUILD_ENVIRONMENT_TYPE == "Staging"
  const val IS_DEV: Boolean = BuildConfig.BUILD_VARIANT_TYPE == "Instrumentation"
}

object Release {
  const val IS_DEBUGGABLE: Boolean = BuildConfig.BUILD_VARIANT_TYPE == "Debug"
  const val IS_INSIDER: Boolean = BuildConfig.APPLICATION_ID == "im.molly.insider"
  const val IS_FOSS: Boolean = BuildConfig.FLAVOR_distribution == "free"
}
