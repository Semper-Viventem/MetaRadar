package f.cking.software.data.database

object DatabaseUtils {

    /**
     * To prevent excessive memory allocations, the maximum value of a host parameter number is SQLITE_MAX_VARIABLE_NUMBER,
     * which defaults to 999 for SQLite versions prior to 3.32.0 (2020-05-22) or 32766 for SQLite versions after 3.32.0.
     *
     * https://www.sqlite.org/limits.html
     */
    const val MAX_SQL_VARIABLES = 32766
}