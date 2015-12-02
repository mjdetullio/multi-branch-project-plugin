/*
 * The MIT License
 *
 * Copyright (c) 2014, Matthew DeTullio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.mjdetullio.jenkins.plugins.multibranch;

import javax.servlet.ServletException;

import hudson.DescriptorExtensionList;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Prevents configuration of {@link AbstractMultiBranchProject}s from bleeding
 * into their template projects when the request is passed to the template's
 * {@link hudson.model.AbstractProject#doConfigSubmit(StaplerRequest, StaplerResponse)}
 * method.
 *
 * @author Matthew DeTullio
 */
public final class TemplateStaplerRequestWrapper extends RequestImpl {
	/*package*/ TemplateStaplerRequestWrapper(StaplerRequest request)
			throws ServletException {
		/*
		 * Ugly casts to RequestImpl... but should be ok since it will throw
		 * errors, which we want anyway if it's not that type.
		 */
		super(request.getStapler(), request, ((RequestImpl) request).ancestors,
				((RequestImpl) request).tokens);

		// Remove some fields that we don't want to send to the template
		JSONObject json = getSubmittedForm();
		json.remove("name");
		json.remove("description");
		json.remove("displayNameOrNull");
	}

	/**
	 * Overrides certain parameter names with certain values needed when setting
	 * the configuration for template projects.  Otherwise, relies on the
	 * standard implementation.
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public String getParameter(String name) {
		// Sanitize the following parameters
		if ("name".equals(name)) {
			// Don't set the name
			return null;
		} else if ("description".equals(name)) {
			// Don't set the description
			return null;
		} else if ("disable".equals(name)) {
			// Mark disabled
			return "";
		}

		/*
		 * Parameters for conflicting triggers should return null if the
		 * corresponding JSON was not provided.  Otherwise, NPEs occur when
		 * trying to update the triggers for the template project.
		 */
		DescriptorExtensionList<Trigger<?>,TriggerDescriptor> triggerDescriptors = Trigger.all();
		for (TriggerDescriptor triggerDescriptor : triggerDescriptors) {
			String jsonName = triggerDescriptor.getJsonSafeClassName();

			try {
				if (name.equals(jsonName)
						&& getSubmittedForm().getJSONObject(jsonName).isNullObject()) {
					return null;
				}
			} catch (ServletException e) {
				throw new IllegalStateException(
						"Exception getting data from submitted JSON", e);
			}
		}

		// Fallback to standard functionality
		return super.getParameter(name);
	}

	/**
	 * Overrides the form with a sanitized version.
	 * <br>
	 * {@inheritDoc}
	 */
	@Override
	public JSONObject getSubmittedForm() throws ServletException {
		JSONObject json = super.getSubmittedForm();

		// Don't set the name
		json.remove("name");

		// Don't set the display name
		json.remove("displayNameOrNull");

		// Don't set the description
		json.remove("description");

		// Don't change the disabled state
		json.remove("disable");

		// Don't send conflicting triggers
		json.remove("syncBranchesTriggers");

		return json;
	}
}
