<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
         "http://java.sun.com/products/javahelp/helpset_2_0.dtd">
<helpset version="2.0">
   <!-- title --> 
   <title>GPU Help</title>
			
   <!-- maps --> 
   <maps>
     <homeID>top</homeID>
     <mapref location="map.jhm" />
   </maps>
	
   <!-- presentation windows -->

   <presentation default=true>
       <name>main window</name>
       <size width="900" height="700" /> 
       <location x="200" y="10" />
   </presentation>

   <!-- implementation section -->
   <impl>
      <helpsetregistry helpbrokerclass="javax.help.DefaultHelpBroker" />
      <viewerregistry viewertype="text/html" 
         viewerclass="com.sun.java.help.impl.CustomKit" />
      <viewerregistry viewertype="text/xml" 
         viewerclass="com.sun.java.help.impl.CustomXMLKit" />
   </impl>
</helpset>
