package ai.rojan.designlab.data.demo

object DemoWalletRepository {
    const val BALANCE = 2_450_000

    val transactions: List<DemoWalletTransaction> = listOf(
        DemoWalletTransaction("txn_01", "شارژ کیف پول", 2_000_000, isCredit = true, dateLabel = "۲۰ تیر"),
        DemoWalletTransaction("txn_02", "پرداخت - سالن رویا", 690_000, isCredit = false, dateLabel = "۱۸ تیر"),
        DemoWalletTransaction("txn_03", "بازگشت وجه", 200_000, isCredit = true, dateLabel = "۱۰ تیر"),
        DemoWalletTransaction("txn_04", "پرداخت - استودیو luxe", 1_500_000, isCredit = false, dateLabel = "۲۸ خرداد"),
        DemoWalletTransaction("txn_05", "پاداش دعوت دوستان", 150_000, isCredit = true, dateLabel = "۲۰ خرداد"),
    )
}
