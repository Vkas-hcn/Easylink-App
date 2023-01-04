package com.vkas.easylinkapp.view.vpnlist

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easylinkapp.BR
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.base.BaseActivity
import com.vkas.easylinkapp.bean.ElVpnBean
import com.vkas.easylinkapp.databinding.ActivityListElBinding
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.utils.KLog
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job

class VpnList : BaseActivity<ActivityListElBinding, VpnListViewModel>() {
    private lateinit var selectAdapter: VpnListAdapter
    private var elServiceBeanList: MutableList<ElVpnBean> = ArrayList()
    private lateinit var adBean: ElVpnBean

    private var jobBackEl: Job? = null

    //选中服务器
    private lateinit var checkSkServiceBean: ElVpnBean
    private lateinit var checkSkServiceBeanClick: ElVpnBean

    // 是否连接
    private var whetherToConnect = false
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_list_el
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        checkSkServiceBean = ElVpnBean()
        whetherToConnect = bundle?.getBoolean(Constant.WHETHER_EL_CONNECTED) == true
        checkSkServiceBean = JsonUtil.fromJson(
            bundle?.getString(Constant.CURRENT_EL_SERVICE),
            object : TypeToken<ElVpnBean?>() {}.type
        )
        checkSkServiceBeanClick = checkSkServiceBean
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.selectTitle.imgBack.setImageResource(R.drawable.ic_title_back)
        binding.selectTitle.tvTitle.text = getString(R.string.locations)
        binding.selectTitle.tvRight.visibility = View.GONE
        binding.selectTitle.imgBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        super.initData()
        initSelectRecyclerView()
        viewModel.getServerListData()
    }

    override fun initViewObservable() {
        super.initViewObservable()
        getServerListData()
    }

    private fun getServerListData() {
        viewModel.liveServerListData.observe(this, {
            echoServer(it)
        })
    }

    private fun initSelectRecyclerView() {
        selectAdapter = VpnListAdapter(elServiceBeanList)
        val layoutManager =
            GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)

        binding.recyclerSelect.layoutManager = layoutManager
        binding.recyclerSelect.adapter = selectAdapter
        selectAdapter.setOnItemClickListener { _, _, pos ->
            run {
                selectServer(pos)
            }
        }
    }

    /**
     * 选中服务器
     */
    private fun selectServer(position: Int) {
        if (elServiceBeanList[position].el_ip == checkSkServiceBeanClick.el_ip && elServiceBeanList[position].el_best == checkSkServiceBeanClick.el_best) {
            if (!whetherToConnect) {
                finish()
                LiveEventBus.get<ElVpnBean>(Constant.NOT_CONNECTED_EL_RETURN)
                    .post(checkSkServiceBean)
            }
            return
        }
        elServiceBeanList.forEachIndexed { index, _ ->
            elServiceBeanList[index].el_check = position == index
            if (elServiceBeanList[index].el_check == true) {
                checkSkServiceBean = elServiceBeanList[index]
            }
        }
        selectAdapter.notifyDataSetChanged()
        showDisconnectDialog()
    }

    /**
     * 回显服务器
     */
    private fun echoServer(it: MutableList<ElVpnBean>) {
        elServiceBeanList = it
        elServiceBeanList.forEachIndexed { index, _ ->
            if (checkSkServiceBeanClick.el_best == true) {
                elServiceBeanList[0].el_check = true
            } else {
                elServiceBeanList[index].el_check =
                    elServiceBeanList[index].el_ip == checkSkServiceBeanClick.el_ip
                elServiceBeanList[0].el_check = false
            }
        }
        KLog.e("TAG", "elServiceBeanList=${JsonUtil.toJson(elServiceBeanList)}")
        selectAdapter.setList(elServiceBeanList)
    }

    /**
     * 返回主页
     */
    private fun returnToHomePage() {
        finish()
    }

    /**
     * 是否断开连接
     */
    private fun showDisconnectDialog() {
        if (!whetherToConnect) {
            finish()
            LiveEventBus.get<ElVpnBean>(Constant.NOT_CONNECTED_EL_RETURN)
                .post(checkSkServiceBean)
            return
        }
        val dialog: AlertDialog? = AlertDialog.Builder(this)
            .setTitle("Are you sure to disconnect current server")
            //设置对话框的按钮
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
                elServiceBeanList.forEachIndexed { index, _ ->
                    elServiceBeanList[index].el_check =
                        (elServiceBeanList[index].el_ip == checkSkServiceBeanClick.el_ip && elServiceBeanList[index].el_best == checkSkServiceBeanClick.el_best)
                }
                selectAdapter.notifyDataSetChanged()
            }
            .setPositiveButton("DISCONNECT") { dialog, _ ->
                dialog.dismiss()
                finish()
                LiveEventBus.get<ElVpnBean>(Constant.CONNECTED_EL_RETURN)
                    .post(checkSkServiceBean)
            }.create()

        val params = dialog!!.window!!.attributes
        params.width = 200
        params.height = 200
        dialog.window!!.attributes = params
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

}