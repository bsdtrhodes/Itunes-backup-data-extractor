module me.bsdtrhodes.iExtractor {
    exports me.bsdtrhodes.iExtractor;

    requires transitive dd.plist;
    requires java.logging;
    requires transitive java.sql;
    requires java.xml;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires org.bouncycastle.provider;
    requires org.xerial.sqlitejdbc;
	requires java.desktop;

    opens me.bsdtrhodes.iExtractor to javafx.fxml, javafx.web,
    	javafx.graphics;
}