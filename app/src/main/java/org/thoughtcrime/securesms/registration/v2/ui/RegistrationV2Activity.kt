/*
 * Copyright 2024 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.registration.v2.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.ActivityNavigator
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.BaseActivity
import org.thoughtcrime.securesms.MainActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.lock.v2.CreateSvrPinActivity
import org.thoughtcrime.securesms.pin.PinRestoreActivity
import org.thoughtcrime.securesms.profiles.AvatarHelper
import org.thoughtcrime.securesms.profiles.edit.CreateProfileActivity
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.registration.SmsRetrieverReceiver
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme

/**
 * Activity to hold the entire registration process.
 */
class RegistrationV2Activity : BaseActivity() {

  private val TAG = Log.tag(RegistrationV2Activity::class.java)

  private val dynamicTheme = DynamicNoActionBarTheme()
  val sharedViewModel: RegistrationV2ViewModel by viewModels()

  private var smsRetrieverReceiver: SmsRetrieverReceiver? = null

  init {
    lifecycle.addObserver(SmsRetrieverObserver())
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    dynamicTheme.onCreate(this)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_registration_navigation_v2)

    sharedViewModel.checkpoint.observe(this) {
      if (it >= RegistrationCheckpoint.LOCAL_REGISTRATION_COMPLETE) {
        handleSuccessfulVerify()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    dynamicTheme.onResume(this)
  }

  private fun handleSuccessfulVerify() {
    if (SignalStore.misc().hasLinkedDevices) {
      SignalStore.misc().shouldShowLinkedDevicesReminder = sharedViewModel.isReregister
    }

    if (SignalStore.storageService().needsAccountRestore()) {
      Log.i(TAG, "Performing pin restore.")
      startActivity(Intent(this, PinRestoreActivity::class.java))
      finish()
    } else {
      val isProfileNameEmpty = Recipient.self().profileName.isEmpty
      val isAvatarEmpty = !AvatarHelper.hasAvatar(this, Recipient.self().id)
      val needsProfile = isProfileNameEmpty || isAvatarEmpty
      val needsPin = !sharedViewModel.hasPin()

      Log.i(TAG, "Pin restore flow not required. Profile name: $isProfileNameEmpty | Profile avatar: $isAvatarEmpty | Needs PIN: $needsPin")

      SignalStore.internalValues().setForceEnterRestoreV2Flow(true)

      if (!needsProfile && !needsPin) {
        sharedViewModel.completeRegistration()
      }
      sharedViewModel.setInProgress(false)

      val startIntent = MainActivity.clearTop(this).apply {
        if (needsPin) {
          putExtra("next_intent", CreateSvrPinActivity.getIntentForPinCreate(this@RegistrationV2Activity))
        }

        if (needsProfile) {
          putExtra("next_intent", CreateProfileActivity.getIntentForUserProfile(this@RegistrationV2Activity))
        }
      }

      Log.d(TAG, "Launching ${startIntent.component}")
      startActivity(startIntent)
      finish()
      ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
  }

  private inner class SmsRetrieverObserver : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
      smsRetrieverReceiver = SmsRetrieverReceiver(application)
      smsRetrieverReceiver?.registerReceiver()
    }

    override fun onDestroy(owner: LifecycleOwner) {
      smsRetrieverReceiver?.unregisterReceiver()
      smsRetrieverReceiver = null
    }
  }

  companion object {

    @JvmStatic
    fun newIntentForNewRegistration(context: Context, originalIntent: Intent): Intent {
      return Intent(context, RegistrationV2Activity::class.java).apply {
        setData(originalIntent.data)
      }
    }
  }
}
