<%--
 Copyright (c) 2009 IBM Corporation and others.
 All rights reserved. This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html
 
 Contributors:
     IBM Corporation - initial API and implementation
--%>
<%@ include file="fheader.jsp"%>

<% 
	LayoutData data = new LayoutData(application,request, response);
    FrameData frameData = new FrameData(application,request, response);
	WebappPreferences prefs = data.getPrefs();
	AbstractFrame frame = frameData.getHelpToolbarFrame();
%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%=ServletResources.getString("helpToolbarFrame", request)%></title>
</head>
<frameset id="helpToolbarFrameset" cols="<%=frameData.getHelpToolbarFrameSizes()%>" frameborder=0" framespacing="0" border="0" spacing="0">
<% if (isRTL) {
if(null != frame){%>
    <frame name="<%=frame.getName()%>" src="<%= frameData.getUrl(frame) %>" <%=frame.getFrameAttributes()%> >
<%}%>   	
    <frame name="SearchFrame" title="<%=ServletResources.getString("SearchFrame", request)%>" src='<%="search.jsp"+UrlUtil.htmlEncode(data.getQuery())%>' marginwidth="0" marginheight="0" scrolling="no" frameborder="0" noresize=0>
<%} else {%>
   	<frame name="SearchFrame" title="<%=ServletResources.getString("SearchFrame", request)%>" src='<%="search.jsp"+UrlUtil.htmlEncode(data.getQuery())%>' marginwidth="0" marginheight="0" scrolling="no" frameborder="0" noresize=0>
<%if(null != frame){%>    
    <frame name="<%=frame.getName()%>" src="<%= frameData.getUrl(frame) %>" <%=frame.getFrameAttributes()%> >
<%} 
}%>	
</frameset>
</html>

