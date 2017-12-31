package com.udeshcoffee.android.ui.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.PagerAdapter
import java.util.*

/**
 * Created by Udathari on 8/26/2017.
 */
class FragAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    private val mFragmentList = ArrayList<Fragment>()
    private val tabTitles = ArrayList<String>()

    override fun getItem(position: Int): Fragment {
        return mFragmentList[position]
    }

    fun addFragment(fragment: Fragment, title: String) {
        mFragmentList.add(fragment)
        tabTitles.add(title)
    }

    override fun getCount(): Int {
        return mFragmentList.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabTitles[position]
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }
}
