package com.focuslock.app

import android.content.Context
import android.graphics.Color

/** One colour palette. accent = orange-role (pin, "Lock", highlights), primary = filled controls (play, selected tile), ink = digits/icons. */
class AppTheme(
    val id: String, val name: String,
    val bg: Int, val tile: Int, val card: Int,
    val ink: Int, val txt: Int,
    val accent: Int, val primary: Int,
    val gray: Int, val line: Int,
    val soft: Int, val l1: Int, val l2: Int,
    val primaryTop: Int, val hill: Int,
    val clkA: Int, val clkZ: Int,
    val s1: Int, val s2: Int, val s3: Int, val s4: Int, val sun: Int
)

object Themes {
    // Default = the original dark grey + orange look, unchanged.
    val DEFAULT = AppTheme(
        "default", "Default",
        Color.parseColor("#F6F3F3"), Color.parseColor("#EBE6E6"), Color.parseColor("#FCFAFA"),
        Color.parseColor("#403B39"), Color.parseColor("#2E2927"),
        Color.parseColor("#FF6A0A"), Color.parseColor("#403B39"),
        Color.parseColor("#B5AEAC"), Color.parseColor("#C9C2C0"),
        Color.parseColor("#F2B89C"), Color.parseColor("#F7C4A5"), Color.parseColor("#FA9A5C"),
        Color.parseColor("#403B39"), 0,
        Color.parseColor("#4D4846"), Color.parseColor("#F45516"),
        Color.parseColor("#DAD4D8"), Color.parseColor("#B8B1B8"), Color.parseColor("#8E8890"), Color.parseColor("#6E6870"),
        Color.parseColor("#FFB066")
    )

    val all: List<AppTheme> = listOf(
        DEFAULT,
        AppTheme("autumn", "Autumn", 0xFFFEFAF6.toInt(), 0xFFFAEFE9.toInt(), 0xFFFEFCFB.toInt(), 0xFF4E3C30.toInt(), 0xFF3A2D24.toInt(), 0xFFD27935.toInt(), 0xFFAB622B.toInt(), 0xFFAEA39C.toInt(), 0xFFDBCFC8.toInt(), 0xFFEBC3A4.toInt(), 0xFFEECCB2.toInt(), 0xFFDFA172.toInt(), 0xFFC08960.toInt(), 0xFFF4DCCC.toInt(), 0xFF4E3C30.toInt(), 0xFFAB622B.toInt(), 0xFFF3DBCB.toInt(), 0xFFE9BE9F.toInt(), 0xFFA46333.toInt(), 0xFF835432.toInt(), 0xFFEBC3A4.toInt()),
        AppTheme("sakura", "Sakura", 0xFFFAF1E4.toInt(), 0xFFFCEFF4.toInt(), 0xFFFEFCFD.toInt(), 0xFF3C2A30.toInt(), 0xFF2D2024.toInt(), 0xFFD55476.toInt(), 0xFFC04C6A.toInt(), 0xFFA69B9F.toInt(), 0xFFD9CCD1.toInt(), 0xFFECB2C1.toInt(), 0xFFEFBECB.toInt(), 0xFFE2879F.toInt(), 0xFFD07990.toInt(), 0xFFF6D6E0.toInt(), 0xFF3C2A30.toInt(), 0xFFC04C6A.toInt(), 0xFFF4D4DD.toInt(), 0xFFEBAEBF.toInt(), 0xFF9F455E.toInt(), 0xFF793B4C.toInt(), 0xFFECB2C1.toInt()),
        AppTheme("japan", "Japan", 0xFFFDF9F5.toInt(), 0xFFF6EFED.toInt(), 0xFFFEFCFC.toInt(), 0xFF3C241E.toInt(), 0xFF2D1B16.toInt(), 0xFFAE3C3D.toInt(), 0xFFAE3C3D.toInt(), 0xFFA69995.toInt(), 0xFFD5CAC8.toInt(), 0xFFDBA7A8.toInt(), 0xFFE0B5B5.toInt(), 0xFFC67677.toInt(), 0xFFC26D6E.toInt(), 0xFFEAD2D1.toInt(), 0xFF3C241E.toInt(), 0xFFAE3C3D.toInt(), 0xFFEBCFCE.toInt(), 0xFFD9A3A3.toInt(), 0xFF863432.toInt(), 0xFF6A2E2A.toInt(), 0xFFDBA7A8.toInt()),
        AppTheme("rajasthan", "Rajasthan", 0xFFFCF6F2.toInt(), 0xFFF9EEE6.toInt(), 0xFFFEFBFA.toInt(), 0xFF543624.toInt(), 0xFF3F281B.toInt(), 0xFFB0604A.toInt(), 0xFFB0604A.toInt(), 0xFFB0A095.toInt(), 0xFFDBCDC3.toInt(), 0xFFDBB7AE.toInt(), 0xFFE1C3BA.toInt(), 0xFFC89080.toInt(), 0xFFC48877.toInt(), 0xFFEDD7CD.toInt(), 0xFF543624.toInt(), 0xFFB0604A.toInt(), 0xFFEBD5CD.toInt(), 0xFFDAB2A6.toInt(), 0xFF90513D.toInt(), 0xFF794733.toInt(), 0xFFDBB7AE.toInt()),
        AppTheme("desert", "Desert", 0xFFF9F0E8.toInt(), 0xFFFBEFE4.toInt(), 0xFFFEFCFB.toInt(), 0xFF423630.toInt(), 0xFF322824.toInt(), 0xFFB26B2E.toInt(), 0xFFA9662C.toInt(), 0xFFA9A19C.toInt(), 0xFFDACEC4.toInt(), 0xFFDCBCA1.toInt(), 0xFFE2C7B0.toInt(), 0xFFC9976D.toInt(), 0xFFBF8C61.toInt(), 0xFFEFDAC7.toInt(), 0xFF423630.toInt(), 0xFFA9662C.toInt(), 0xFFECD9C8.toInt(), 0xFFDBB99B.toInt(), 0xFF8B582F.toInt(), 0xFF6F4B2F.toInt(), 0xFFDCBCA1.toInt()),
        AppTheme("santorini", "Santorini", 0xFFFBF4E6.toInt(), 0xFFF6ECE8.toInt(), 0xFFFDFCFB.toInt(), 0xFF483024.toInt(), 0xFF36241B.toInt(), 0xFF9B5E48.toInt(), 0xFF9B5E48.toInt(), 0xFFAA9D98.toInt(), 0xFFD7CAC5.toInt(), 0xFFD2B7AD.toInt(), 0xFFD9C2B9.toInt(), 0xFFB98E7F.toInt(), 0xFFB48676.toInt(), 0xFFE7D5CE.toInt(), 0xFF483024.toInt(), 0xFF9B5E48.toInt(), 0xFFE6D5D0.toInt(), 0xFFD0B2A8.toInt(), 0xFF7E4E3B.toInt(), 0xFF694232.toInt(), 0xFFD2B7AD.toInt()),
        AppTheme("rome", "Rome", 0xFFFDFAF7.toInt(), 0xFFF5F0EE.toInt(), 0xFFFDFCFC.toInt(), 0xFF4E372E.toInt(), 0xFF3A2922.toInt(), 0xFF7A5648.toInt(), 0xFF7A5648.toInt(), 0xFFADA19D.toInt(), 0xFFD7CFCB.toInt(), 0xFFC3B3AD.toInt(), 0xFFCCBFB9.toInt(), 0xFFA2897F.toInt(), 0xFF9B8076.toInt(), 0xFFE1D7D3.toInt(), 0xFF4E372E.toInt(), 0xFF7A5648.toInt(), 0xFFDFD4D0.toInt(), 0xFFC1AFA8.toInt(), 0xFF6B4B3F.toInt(), 0xFF604338.toInt(), 0xFFC3B3AD.toInt()),
        AppTheme("spring", "Spring", 0xFFF5F4E9.toInt(), 0xFFF3F4EE.toInt(), 0xFFFDFDFD.toInt(), 0xFF42423C.toInt(), 0xFF32322D.toInt(), 0xFF7D965F.toInt(), 0xFF667A4D.toInt(), 0xFFA8A7A4.toInt(), 0xFFD3D4CE.toInt(), 0xFFC4D0B7.toInt(), 0xFFCED7C2.toInt(), 0xFFA4B68F.toInt(), 0xFF8C9B7A.toInt(), 0xFFE0E5D7.toInt(), 0xFF42423C.toInt(), 0xFF667A4D.toInt(), 0xFFDFE4D7.toInt(), 0xFFC2CDB4.toInt(), 0xFF687953.toInt(), 0xFF5A644A.toInt(), 0xFFC4D0B7.toInt()),
        AppTheme("cabin", "Cabin", 0xFFFBFAF5.toInt(), 0xFFF5F0ED.toInt(), 0xFFFEFDFD.toInt(), 0xFF263A2C.toInt(), 0xFF1C2C21.toInt(), 0xFF2F5D3F.toInt(), 0xFF2F5D3F.toInt(), 0xFF9CA39D.toInt(), 0xFFD0CFCA.toInt(), 0xFFA1B6A9.toInt(), 0xFFB0C1B6.toInt(), 0xFF6D8E79.toInt(), 0xFF63866F.toInt(), 0xFFD5D8D1.toInt(), 0xFF263A2C.toInt(), 0xFF2F5D3F.toInt(), 0xFFCFD7D0.toInt(), 0xFFA0B3A5.toInt(), 0xFF2C5138.toInt(), 0xFF2A4834.toInt(), 0xFFA1B6A9.toInt()),
        AppTheme("lake", "Lake", 0xFFFCFBFA.toInt(), 0xFFEFF5FA.toInt(), 0xFFFCFDFE.toInt(), 0xFF303C42.toInt(), 0xFF242D32.toInt(), 0xFF278995.toInt(), 0xFF237C86.toInt(), 0xFF9EA4A9.toInt(), 0xFFCDD4D9.toInt(), 0xFF9ECACF.toInt(), 0xFFADD2D7.toInt(), 0xFF68ACB5.toInt(), 0xFF5A9CA5.toInt(), 0xFFCFE4EA.toInt(), 0xFF303C42.toInt(), 0xFF237C86.toInt(), 0xFFCAE1E6.toInt(), 0xFF9AC7CE.toInt(), 0xFF2A6E78.toInt(), 0xFF2C5B63.toInt(), 0xFF9ECACF.toInt()),
        AppTheme("ocean", "Ocean", 0xFFF2F5ED.toInt(), 0xFFE9F3F2.toInt(), 0xFFFDFEFE.toInt(), 0xFF2A363C.toInt(), 0xFF20282D.toInt(), 0xFF1C6A6E.toInt(), 0xFF1C6A6E.toInt(), 0xFF9CA3A6.toInt(), 0xFFC7D1D1.toInt(), 0xFF99BCBE.toInt(), 0xFFA9C6C8.toInt(), 0xFF60979A.toInt(), 0xFF558F92.toInt(), 0xFFC8DDDD.toInt(), 0xFF2A363C.toInt(), 0xFF1C6A6E.toInt(), 0xFFC8DCDD.toInt(), 0xFF96BABC.toInt(), 0xFF21585C.toInt(), 0xFF244B50.toInt(), 0xFF99BCBE.toInt()),
        AppTheme("underwater", "Underwater", 0xFFFAFAF1.toInt(), 0xFFEEF6F4.toInt(), 0xFFFDFDF8.toInt(), 0xFF1F3A3D.toInt(), 0xFF17302F.toInt(), 0xFF2E8C8F.toInt(), 0xFF2A7F82.toInt(), 0xFF9BA5A3.toInt(), 0xFFCCD7D2.toInt(), 0xFF9CCFCE.toInt(), 0xFFAAD6D4.toInt(), 0xFF63AEAF.toInt(), 0xFF6FB4B4.toInt(), 0xFFCFE6E3.toInt(), 0xFF1F3A3D.toInt(), 0xFF2A7F82.toInt(), 0xFFCBE5E2.toInt(), 0xFF9ACBCB.toInt(), 0xFF256E72.toInt(), 0xFF1F5A5E.toInt(), 0xFF9CCFCE.toInt()),
        AppTheme("ink", "Ink", 0xFFFDFAF7.toInt(), 0xFFEBF0F0.toInt(), 0xFFFDFDFD.toInt(), 0xFF1B2B2A.toInt(), 0xFF142020.toInt(), 0xFF2A7F6F.toInt(), 0xFF2A7F6F.toInt(), 0xFF969D9D.toInt(), 0xFFC6CDCC.toInt(), 0xFF9FC5BE.toInt(), 0xFFAECEC8.toInt(), 0xFF6AA59A.toInt(), 0xFF5F9F93.toInt(), 0xFFCCDEDB.toInt(), 0xFF1B2B2A.toInt(), 0xFF2A7F6F.toInt(), 0xFFCDDFDC.toInt(), 0xFF9DC3BC.toInt(), 0xFF256257.toInt(), 0xFF214D46.toInt(), 0xFF9FC5BE.toInt()),
        AppTheme("rain", "Rain", 0xFFF2F2E8.toInt(), 0xFFF0F2F3.toInt(), 0xFFFCFCFC.toInt(), 0xFF3C4248.toInt(), 0xFF2D3236.toInt(), 0xFF5D86AD.toInt(), 0xFF54799C.toInt(), 0xFFA3A6A8.toInt(), 0xFFD0D2D4.toInt(), 0xFFB6C9DA.toInt(), 0xFFC1D1E0.toInt(), 0xFF8EAAC6.toInt(), 0xFF7F9AB5.toInt(), 0xFFD8E1E8.toInt(), 0xFF3C4248.toInt(), 0xFF54799C.toInt(), 0xFFD5DEE7.toInt(), 0xFFB2C4D6.toInt(), 0xFF516E8A.toInt(), 0xFF495D70.toInt(), 0xFFB6C9DA.toInt()),
        AppTheme("korea", "Korea", 0xFFFCF6F1.toInt(), 0xFFF4EFEE.toInt(), 0xFFFEFCFB.toInt(), 0xFF242A3C.toInt(), 0xFF1B202D.toInt(), 0xFF3C5078.toInt(), 0xFF3C5078.toInt(), 0xFF9B9BA2.toInt(), 0xFFCFCCCE.toInt(), 0xFFA7B0C2.toInt(), 0xFFB5BCCC.toInt(), 0xFF7684A0.toInt(), 0xFF6D7C9A.toInt(), 0xFFD7D6DB.toInt(), 0xFF242A3C.toInt(), 0xFF3C5078.toInt(), 0xFFD2D3DA.toInt(), 0xFFA6ACBD.toInt(), 0xFF344363.toInt(), 0xFF2E3954.toInt(), 0xFFA7B0C2.toInt())
    )

    fun byId(id: String?): AppTheme = all.firstOrNull { it.id == id } ?: DEFAULT

    fun apply(t: AppTheme) {
        C_BG = t.bg; C_TILE = t.tile; C_CARD = t.card
        C_DARK = t.ink; C_TXT = t.txt
        C_ORANGE = t.accent; C_PRIMARY = t.primary
        C_GRAY = t.gray; C_LINE = t.line
        C_SOFT = t.soft; C_L1 = t.l1; C_L2 = t.l2
        C_PRIMARY_TOP = t.primaryTop; C_HILL = t.hill
        C_THEME_ID = t.id
        C_CLK_A = t.clkA; C_CLK_Z = t.clkZ
        C_S1 = t.s1; C_S2 = t.s2; C_S3 = t.s3; C_S4 = t.s4; C_SUN = t.sun
    }

    fun savedId(ctx: Context): String =
        ctx.getSharedPreferences("focuslock_theme", Context.MODE_PRIVATE).getString("id", "default") ?: "default"

    fun save(ctx: Context, id: String) {
        ctx.getSharedPreferences("focuslock_theme", Context.MODE_PRIVATE).edit().putString("id", id).apply()
    }

    fun load(ctx: Context) = apply(byId(savedId(ctx)))
}
