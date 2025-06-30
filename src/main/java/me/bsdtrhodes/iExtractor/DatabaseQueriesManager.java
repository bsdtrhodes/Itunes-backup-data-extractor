/*-
 * Copyright (c) 2025 Tom Rhodes. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package me.bsdtrhodes.iExtractor;

/*
*****************************************************************************
* Strings for various SQLite queries.                                       *
*****************************************************************************
*/

public class DatabaseQueriesManager {

	/* These strings are used in the BackupManager. */
		public static String getContactsQuery() {
			/* The Contacts list building query is kind of crazy! */
			return "select ABPerson.ROWID, ABPerson.first,"
					+ " ABPerson.last,  ABPerson.Organization as organization,"
					+ " ABPerson.Department as department,"
					+ " DATETIME(ABPerson.Birthday - 63113904000, 'unixepoch', 'localtime') as"
					+ " birthday, ABPerson.JobTitle as jobtitle, ABPerson.Note,"
					+ " ABPerson.Nickname, DATETIME(ABPerson.CreationDate +"
					+ " STRFTIME('%s', '2001-01-01 00:00:00'), 'unixepoch',"
					+ " 'localtime') AS Created,"
					+ " DATETIME(ABPerson.ModificationDate +"
					+ " STRFTIME('%s', '2001-01-01 00:00:00'), 'unixepoch',"
					+ " 'localtime') AS Modified, (select value from"
					+ " ABMultiValue where property = 3 AND record_id ="
					+ " ABPerson.ROWID AND label = (select ROWID from"
					+ " ABMultiValueLabel where value = '_$!<Work>!$_')) as"
					+ " phone_work, (select value from ABMultiValue where"
					+ " property = 3 and record_id = ABPerson.ROWID and"
					+ " label = (select ROWID from ABMultiValueLabel where"
					+ " value = '_$!<Mobile>!$_')) as phone_mobile, (select"
					+ " value from ABMultiValue where property = 3 and"
					+ " record_id = ABPerson.ROWID and label ="
					+ " (select ROWID from"
					+ " ABMultiValueLabel where value = '_$!<Home>!$_')) as"
					+ " phone_home, (select value from ABMultiValue where"
					+ " property = 4 and record_id = ABPerson.ROWID and label is"
					+ " null) as email, (select value from ABMultiValueEntry"
					+ " where parent_id in (select ROWID from ABMultiValue where"
					+ " record_id = ABPerson.ROWID) and key = (select ROWID"
					+ " from ABMultiValueEntryKey where lower(value) = 'street'))"
					+ " as address, (select value from ABMultiValueEntry where"
					+ " parent_id in (select ROWID from ABMultiValue where"
					+ " record_id = ABPerson.ROWID) and key = (select ROWID from"
					+ " ABMultiValueEntryKey where lower(value) = 'city')) as"
					+ " city from ABPerson order by ABPerson.ROWID";
	    }

	    /* In the future, we may be able to use AND flags = 2 only? */
	    public static String getDirectoryQuery() {
	    	return "SELECT relativePath FROM Files WHERE domain ="
					+ " 'CameraRollDomain' AND relativePath LIKE '%DCIM%' AND"
					+ " relativePath NOT LIKE '%Thumbnails%' AND relativePath NOT"
					+ " LIKE '%IMG%' AND relativePath NOT LIKE '%Mutations%'"
					+ " AND relativePath NOT LIKE '%data%' AND relativePath NOT"
					+ " LIKE '%MISC%'";
	    }

	    public static String getMediaFilesQuery() {
			/*
			 * This query will collect the media list, skipping metadata and
			 * thumbnail files.
			 */
			return "SELECT fileID, relativePath, flags, file FROM Files WHERE"
					+ " domain = 'CameraRollDomain' AND relativePath LIKE"
					+ " '%DCIM%' AND relativePath NOT LIKE '%Thumbnails%' AND"
					+ " relativePath NOT LIKE '%Adjustments%' AND relativePath"
					+ " NOT LIKE '%THM' AND relativePath NOT LIKE '%PhotoData%'"
					+ " AND flags = 1";
	    }

	    public static String getTextMessageQuery() {
	    	return "SELECT COALESCE(m.cache_roomnames, h.id) AS ThreadId,"
	    			+ " m.is_from_me AS IsFromMe, CASE WHEN m.is_from_me ="
	    			+ " 1 THEN m.account ELSE h.id END AS FromPhoneNumber,"
	    			+ " CASE WHEN m.is_from_me = 0 THEN m.account ELSE"
	    			+ " COALESCE(h2.id, h.id) END AS ToPhoneNumber,"
	    			+ " m.service AS Service,"
	    			+ " DATETIME((m.date / 1000000000) + 978307200,"
	    			+ " 'unixepoch', 'localtime') AS TextDate, m.text AS"
	    			+ " MessageText, CASE  WHEN m.cache_roomnames IS NOT"
	    			+ " NULL THEN 1 ELSE 0 END AS IsGroup FROM message AS"
	    			+ " m LEFT JOIN handle AS h ON m.handle_id = h.rowid"
	    			+ " LEFT JOIN chat AS c ON m.cache_roomnames ="
	    			+ " c.room_name LEFT JOIN chat_handle_join AS ch ON"
	    			+ " c.rowid = ch.chat_id LEFT JOIN handle AS h2 ON"
	    			+ " ch.handle_id = h2.rowid WHERE (h2.service IS"
	    			+ " NULL OR m.service = h2.service) ORDER BY 2, m.date";
	    }

	    public static String getVMailDataQuery() {
	    	return "SELECT *, datetime(date, 'unixepoch') AS arrival_date from"
	    		+ " voicemail ORDER BY date ASC";
	    }

	    public static String getVMailFileQuery(String ID) {
	    	return "SELECT fileID, file relativePath FROM FILES WHERE relativePath"
	    		+ " LIKE 'Library/Voicemail/" + ID + ".amr'";
	    }

	    public static String getSafariHistoryQuery() {
	    	return "SELECT DATETIME(history_visits.visit_time +"
	    			+ " STRFTIME('%s','2001-01-01 00:00:00'), 'unixepoch',"
	    			+ " 'localtime') AS visit_times,"
	    			+ " history_visits.title AS site_title,"
	    			+ " history_items.url AS site_url,"
	    			+ " history_items.visit_count"
	    			+ " FROM history_visits"
	    			+ " INNER JOIN history_items"
	    			+ " ON history_visits.history_item = history_items.id"
	    			+ " ORDER BY visit_times DESC";
	    }

	    public static String getCallHistoryQuery() {
	    	return "SELECT DATETIME(ZDATE + STRFTIME('%s', '2001-01-01 00:00:00'),"
	    			+ " 'unixepoch', 'localtime') AS calltime,"
	    			+ " TIME(ZDURATION, 'unixepoch') AS DURATION,"
	    			+ " ZSERVICE_PROVIDER, ZADDRESS AS number, ZORIGINATED FROM"
	    			+ " ZCALLRECORD ORDER BY calltime DESC";
	    }

	    /* This one is used in the EncryptedFile class. */
	    public static String getEncFileQuery() {
	    	return "SELECT fileID, relativePath, flags, file FROM Files WHERE"
	    			+ " fileID LIKE ?";
	    }
}
