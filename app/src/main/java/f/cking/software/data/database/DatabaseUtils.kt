package f.cking.software.data.database

import android.os.Build

object DatabaseUtils {

    /**
     * To prevent excessive memory allocations, the maximum value of a host parameter number is SQLITE_MAX_VARIABLE_NUMBER,
     * which defaults to 999 for SQLite versions prior to 3.32.0 (2020-05-22) or 32766 for SQLite versions after 3.32.0.
     *
     * https://www.sqlite.org/limits.html
     */
    const val MAX_SQL_VARIABLES_NEW = 32766

    /**
     * To prevent excessive memory allocations, the maximum value of a host parameter number is SQLITE_MAX_VARIABLE_NUMBER,
     * which defaults to 999 for SQLite versions prior to 3.32.0 (2020-05-22) or 32766 for SQLite versions after 3.32.0.
     *
     * https://www.sqlite.org/limits.html
     */
    const val MAX_SQL_VARIABLES_OLD = 999

    /**
     * SQL 3.32.0 is supported since Android API 31 and higher.
     *
     * https://developer.android.com/reference/android/database/sqlite/package-summary
     */
    fun getMaxSQLVariablesNumber(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MAX_SQL_VARIABLES_NEW
        } else {
            MAX_SQL_VARIABLES_OLD
        }
    }
}
