<%@ include file="/include.jsp" %>
<%@ page import="org.saulis.Parameters" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>

<tr class="noBorder" >
    <td colspan="2">
        <em>Gerrit Trigger will add a new build to the queue after a new patchset is detected.</em>
    </td>
</tr>

<tr class="noBorder" >
    <td><label for="<%=Parameters.HOST%>">Host: <l:star/></label></td>
    <td>
       <props:textProperty name="<%=Parameters.HOST%>" style="width:100%;"/>
      <span class="smallNote">
          Example: dev.gerrit.com<br/>
      </span>
        <span class="error" id="error_<%=Parameters.HOST%>"></span>
    </td>
</tr>

<tr class="noBorder" >
    <td><label for="<%=Parameters.USERNAME%>">Username: </label></td>
    <td>
       <props:textProperty name="<%=Parameters.USERNAME%>" style="width:10em;"/>
        <span class="error" id="error_<%=Parameters.USERNAME%>"></span>
    </td>
</tr>

<tr class="noBorder" >
    <td><label for="<%=Parameters.KEYPATH%>">Custom Private Key path: </label></td>
    <td>
        <props:textProperty name="<%=Parameters.KEYPATH%>" style="width:10em;"/>
        <span class="error" id="error_<%=Parameters.KEYPATH%>"></span>
    </td>
</tr>

<tr class="noBorder" >
    <td><label for="<%=Parameters.PASSPHRASE%>">Passphrase: </label></td>
    <td>
        <props:passwordProperty name="<%=Parameters.PASSPHRASE%>" style="width:10em;"/>
        <span class="error" id="error_<%=Parameters.PASSPHRASE%>"></span>
    </td>
</tr>


<tr class="noBorder" >
    <td><label for="<%=Parameters.PROJECT%>">Project: </label></td>
    <td>
        <props:textProperty name="<%=Parameters.PROJECT%>" style="width:10em;"/>
    </td>
</tr>

<tr class="noBorder" >
    <td><label for="<%=Parameters.BRANCH%>">Branch: </label></td>
    <td>
        <props:textProperty name="<%=Parameters.BRANCH%>" style="width:10em;"/>
    </td>
</tr>
