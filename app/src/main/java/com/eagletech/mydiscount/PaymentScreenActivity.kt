package com.eagletech.mydiscount

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.amazon.device.drm.LicensingService
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.UserDataResponse
import com.eagletech.mydiscount.data.ManagerData
import com.eagletech.mydiscount.databinding.ActivityPaymentScreenBinding

class PaymentScreenActivity : AppCompatActivity() {
    private lateinit var sBinding: ActivityPaymentScreenBinding
    private lateinit var myData: ManagerData
    private lateinit var currentUserId: String
    private lateinit var currentMarketplace: String


    companion object {
        const val cal5 = "com.eagletech.mydiscount.calculate5"
        const val cal10 = "com.eagletech.mydiscount.calculate10"
        const val cal15 = "com.eagletech.mydiscount.calculate15"
        const val calSub = "com.eagletech.mydiscount.sublongtermcalculate"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sBinding = ActivityPaymentScreenBinding.inflate(layoutInflater)
        setContentView(sBinding.root)
        myData = ManagerData.getInstance(this)
        setupIAPOnCreate()
        setClickItems()

    }

    private fun setClickItems() {
        sBinding.cal5.setOnClickListener {
            myData.addData(2)
            PurchasingService.purchase(cal5)
        }
        sBinding.cal10.setOnClickListener {
            PurchasingService.purchase(cal10)
        }
        sBinding.cal15.setOnClickListener {
            PurchasingService.purchase(cal15)
        }
        sBinding.calSub.setOnClickListener {
            PurchasingService.purchase(calSub)
        }
        sBinding.finish.setOnClickListener { finish() }
        sBinding.back.setOnClickListener { finish() }
    }

    private fun setupIAPOnCreate() {
        val purchasingListener: PurchasingListener = object : PurchasingListener {
            override fun onUserDataResponse(response: UserDataResponse) {
                when (response.requestStatus!!) {
                    UserDataResponse.RequestStatus.SUCCESSFUL -> {
                        currentUserId = response.userData.userId
                        currentMarketplace = response.userData.marketplace
                        myData.userId(currentUserId)
                    }

                    UserDataResponse.RequestStatus.FAILED, UserDataResponse.RequestStatus.NOT_SUPPORTED -> Log.v(
                        "IAP SDK",
                        "loading failed"
                    )
                }
            }

            override fun onProductDataResponse(productDataResponse: ProductDataResponse) {
                when (productDataResponse.requestStatus) {
                    ProductDataResponse.RequestStatus.SUCCESSFUL -> {
                        val products = productDataResponse.productData
                        for (key in products.keys) {
                            val product = products[key]
                            Log.v(
                                "Product:", String.format(
                                    "Product: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n",
                                    product!!.title,
                                    product.productType,
                                    product.sku,
                                    product.price,
                                    product.description
                                )
                            )
                        }
                        //get all unavailable SKUs
                        for (s in productDataResponse.unavailableSkus) {
                            Log.v("Unavailable SKU:$s", "Unavailable SKU:$s")
                        }
                    }

                    ProductDataResponse.RequestStatus.FAILED -> Log.v("FAILED", "FAILED")
                    else -> {}
                }
            }

            override fun onPurchaseResponse(purchaseResponse: PurchaseResponse) {
                when (purchaseResponse.requestStatus) {
                    PurchaseResponse.RequestStatus.SUCCESSFUL -> {

                        if (purchaseResponse.receipt.sku == cal5) {
                            myData.addData(5)
                            showInfoDialog()
                        }
                        if (purchaseResponse.receipt.sku == cal10) {
                            myData.addData(10)
                            showInfoDialog()
                        }
                        if (purchaseResponse.receipt.sku == cal15) {
                            myData.addData(15)
                            showInfoDialog()
                        }
                        if (purchaseResponse.receipt.sku == calSub) {
                            myData.isPremium = true
                            showInfoDialog()
                        }
                        PurchasingService.notifyFulfillment(
                            purchaseResponse.receipt.receiptId, FulfillmentResult.FULFILLED
                        )

                        Log.v("FAILED", "FAILED")
                    }

                    PurchaseResponse.RequestStatus.FAILED -> {}
                    else -> {}
                }
            }

            override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
                // Process receipts
                when (response.requestStatus) {
                    PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL -> {
                        for (receipt in response.receipts) {
                            myData.isPremium = !receipt.isCanceled
                        }
                        if (response.hasMore()) {
                            PurchasingService.getPurchaseUpdates(false)
                        }

                    }

                    PurchaseUpdatesResponse.RequestStatus.FAILED -> Log.d("FAILED", "FAILED")
                    else -> {}
                }
            }
        }
        PurchasingService.registerListener(this, purchasingListener)
        Log.d(
            "DetailBuyAct", "Appstore SDK Mode: " + LicensingService.getAppstoreSDKMode()
        )
    }

    private fun showInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dailog_info_buy, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessageBuy)
        val confirmButton = dialogView.findViewById<Button>(R.id.btnConfirmBuy)

        if (myData.isPremium == true) {
            messageTextView.text = "You have successfully registered"
        } else {
            messageTextView.text = "You have ${myData.getData()} uses"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Info")
            .create()

        confirmButton.setOnClickListener {
            dialog.dismiss()
            finish()  // Navigates back to the previous activity
        }

        dialog.show()
    }


    override fun onResume() {
        super.onResume()
        PurchasingService.getUserData()
        val productSkus: MutableSet<String> = HashSet()
        productSkus.add(calSub)
        productSkus.add(cal5)
        productSkus.add(cal10)
        productSkus.add(cal15)
        PurchasingService.getProductData(productSkus)
        PurchasingService.getPurchaseUpdates(false)
    }
}
