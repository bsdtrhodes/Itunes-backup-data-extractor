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
 * BUGS/ISSUES:
 * o If a birthday does not have an attached year, Apple just assigns
 *   the person a birth year as 1604.
 *
 * Some notes. It appears the file, 31bb7ba8914766d4ba40d6dfb6113c8b614be442,
 * is universally used by the Apple iOS system when
 * it comes to storing databases. If that ever
 * changes, the way I did it before was with this SQL query
 * and code:
 * String sql1 = "SELECT fileID From Files WHERE domain = 'HomeDomain'"
	+ "AND relativepath LIKE '%addressbook.sqlitedb%'";
 * PreparedStatement stmt1 = conn.prepareStatement(sql1);
 * ResultSet results1 = stmt1.executeQuery();
 * String dbfname = results1.getString("fileID");
 * String dir = dbfname.substring(0,2);
 * --THE fileID string is the contacts db file, and dir is the dir prefix--
 * String newurl = "jdbc:sqlite:" + bkfloc + File.separator + dir
 *  + File.separator + dbfname;
 */
public class Contact {
	/* Main parts of a contact. */
	private String fName, lName, Org, Dept, Birthday, JobTitle,
	    Note, Nickname, AddDate, ModDate, Phone_Work, Phone_Mobile,
	    Phone_Home, Address, City, Email;

	/*
	 * Some of these entries end up being null, set a few main ones and
	 * use the setters based on null checks. Some phones will be completely
	 * devoid of most values, like mine, handle these anyway.
	 */
	public Contact(String first, String last) {
		this.fName = first;
		this.lName = last;
	}

	/* Getters for this object's data - thank you bash while loop! */
	public String getContactFname() {
		return this.fName;
	}

	public String getContactLname() {
		return this.lName;
	}

	public String getContactOrg() {
		return this.Org;
	}

	public String getContactDept() {
		return this.Dept;
	}

	public String getContactBirthday() {
		return this.Birthday;
	}

	public String getContactJobTitle() {
		return this.JobTitle;
	}

	public String getContactNote() {
		return this.Note;
	}

	public String getContactNickname() {
		return this.Nickname;
	}

	public String getContactAddDate() {
		return this.AddDate;
	}

	public String getContactModDate() {
		return this.ModDate;
	}

	public String getContactPhoneWork() {
		return this.Phone_Work;
	}

	public String getContactPhoneMobile() {
		return this.Phone_Mobile;
	}

	public String getContactPhoneHome() {
		return this.Phone_Home;
	}

	public String getContactAddress() {
		return this.Address;
	}

	public String getContactCity() {
		return this.City;
	}

	public String getContactEmail() {
		return this.Email;
	}

	/* Setters for the contact object - I doubt these will be used. */
	public void setContactOrg(String org) {
		this.Org = org;
	}

	public void setContactDept(String dept) {
		this.Dept = dept;
	}

	public void setContactBirthday(String bday) {
		this.Birthday = bday;
	}

	public void setContactJobTitle(String jtitle) {
		this.JobTitle = jtitle;
	}

	public void SetContactNote(String note) {
		this.Note = note;
	}

	public void setContactNickname(String nname) {
		this.Nickname = nname;
	}

	public void setContactAddDate(String created) {
		this.AddDate = created;
	}

	public void setContactModDate(String modified) {
		this.ModDate = modified;
	}

	public void setContactPhone_Work(String wphone) {
		this.Phone_Work = wphone;
	}

	public void setContactPhone_Mobile(String mphone) {
		this.Phone_Mobile = mphone;
	}

	public void setContactPhone_Home(String hphone) {
		this.Phone_Home = hphone;
	}

	public void setContactAddress(String addy) {
		this.Address = addy;
	}

	public void setContactCity(String city) {
		this.City = city;
	}

	public void setContactEmail(String email) {
		this.Email = email;
	}
}
