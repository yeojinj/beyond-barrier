package com.vd5.beyondb.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.R
import com.vd5.beyondb.databinding.FragmentSettingBinding


// https://velog.io/@changhee09/%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C-%EC%95%B1%EC%9D%98-%ED%99%98%EA%B2%BD%EC%84%A4%EC%A0%95-Preference#6-%EA%B9%83%ED%97%88%EB%B8%8C-%EC%BD%94%EB%93%9C
// https://github.com/dhtmaks2540/PreferenceExample/blob/main/app/src/main/java/kr/co/lee/preferenceexapmle/SettingPreferenceFragment.kt
class SettingsFragment :  Fragment() {

    lateinit var binding : FragmentSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)

        if (savedInstanceState == null) {
            val preferenceFragment = PreferenceList()
            val ft = requireActivity().supportFragmentManager.beginTransaction()
            ft.add(R.id.setting_list, preferenceFragment)
            ft.commit()
        }

        return binding.root
    }

    class PreferenceList : PreferenceFragmentCompat() {
        lateinit var prefs: SharedPreferences
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.main_preference)
            if (rootKey == null) {
                prefs = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
            }
        }
        // 설정 변경 이벤트 처리
        private val prefListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
                when (key) {
                    "tts_speed" -> {
                        val value = prefs.getString("tts_speed","1.2")
                        (activity as MainActivity).textToSpeech?.setSpeechRate(value!!.toFloat())
                    }
                }
            }

        override fun onResume() {
            super.onResume()
            prefs.registerOnSharedPreferenceChangeListener(prefListener)
        }

        override fun onPause() {
            super.onPause()
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        }

    }
}