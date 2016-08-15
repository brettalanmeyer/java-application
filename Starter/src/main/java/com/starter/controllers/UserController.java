package com.starter.controllers;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.starter.dao.UserDAO;
import com.starter.entities.UserEntity;
import com.starter.forms.UserForm;
import com.starter.forms.UserPasswordForm;
import com.starter.validators.ConfirmPasswordValidator;
import com.starter.validators.UserNameAvailableValidator;

@Controller
public class UserController extends BaseController {

	@Autowired
	private UserDAO						userDAO;

	@Autowired
	private PasswordEncoder				passwordEncoder;

	@Autowired
	private UserNameAvailableValidator	userNameAvailableValidator;

	@Autowired
	private ConfirmPasswordValidator	confirmPasswordValidator;

	@InitBinder("UserNameAvailableValidator")
	public void initUserNameAvailableValidatorBinder(WebDataBinder binder) {
		binder.addValidators(this.userNameAvailableValidator);
	}

	@InitBinder("ConfirmPasswordValidator")
	public void initConfirmPasswordValidatorBinder(WebDataBinder binder) {
		binder.addValidators(this.confirmPasswordValidator);
	}

	@RequestMapping(value = "/users", method = RequestMethod.GET)
	public String list(Model model) {
		List<UserEntity> users = this.userDAO.select();
		model.addAttribute("users", users);
		return "users.list";
	}

	@RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
	public String details(@PathVariable UUID id, Model model) {
		UserEntity user = this.userDAO.selectById(id);

		if (user == null) {
			return "redirect:/404";
		}

		model.addAttribute("user", user);
		return "users.details";
	}

	@RequestMapping(value = "/users/new", method = RequestMethod.GET)
	public String newUser(Model model) {
		model.addAttribute("userForm", new UserForm());
		return "users.new";
	}

	@RequestMapping(value = "/users", method = RequestMethod.POST)
	public String createUser(HttpServletResponse response, Model model, @Valid @ModelAttribute("UserNameAvailableValidator") UserForm userForm, BindingResult binding, RedirectAttributes attr) {

		if (binding.hasErrors()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			model.addAttribute("org.springframework.validation.BindingResult.userForm", binding);
			model.addAttribute("user", userForm);
			return "users.new";
		}

		this.userDAO.create(userForm);

		return "redirect:/users";
	}

	@RequestMapping(value = "/users/{id}/edit", method = RequestMethod.GET)
	public String edit(@PathVariable UUID id, HttpServletResponse response, Model model) {
		UserEntity user = this.userDAO.selectById(id);

		if (user == null) {
			return "redirect:/404";
		}

		model.addAttribute("user", user);

		if (model.containsAttribute("userForm")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} else {
			UserForm userForm = (UserForm) this.userDAO.edit(user, UserForm.class);
			model.addAttribute("userForm", userForm);
		}

		return "users.edit";
	}

	@RequestMapping(value = "/users/{id}", method = RequestMethod.PUT)
	public String update(@PathVariable UUID id, Model model, @Valid @ModelAttribute("UserNameAvailableValidator") UserForm userForm, BindingResult binding, RedirectAttributes attr) {
		UserEntity user = this.userDAO.selectById(id);

		if (user == null) {
			return "redirect:/404";
		}

		if (binding.hasErrors()) {
			attr.addFlashAttribute("org.springframework.validation.BindingResult.userForm", binding);
			attr.addFlashAttribute("userForm", userForm);
			return String.format("redirect:/users/%d/edit", id);
		}

		this.userDAO.update(user, userForm);

		return "redirect:/users";
	}

	@RequestMapping(value = "/users/{id}", method = RequestMethod.DELETE)
	public String delete(@PathVariable UUID id) {
		UserEntity user = this.userDAO.selectById(id);
		if (user != null) {
			this.userDAO.delete(user);
		}
		return "redirect:/users";
	}

	@RequestMapping(value = "/users/profile", method = RequestMethod.GET)
	public String profile(Model model) {
		return this.details(this.userDAO.getCurrentUser().getId(), model);
	}

	@RequestMapping(value = "/users/change-password", method = RequestMethod.GET)
	public String changePassword(HttpServletResponse response, Model model) {
		UserEntity user = this.userDAO.getCurrentUser();

		if (user == null) {
			return "redirect:/404";
		}

		model.addAttribute("user", user);

		if (model.containsAttribute("userPasswordForm")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} else {
			UserPasswordForm userPasswordForm = (UserPasswordForm) this.userDAO.edit(user, UserPasswordForm.class);
			model.addAttribute("userPasswordForm", userPasswordForm);
		}

		return "users.change-password";
	}

	@RequestMapping(value = "/users/change-password", method = RequestMethod.PATCH)
	public String updatePassword(Model model, @Valid @ModelAttribute("ConfirmPasswordValidator") UserPasswordForm userPasswordForm, BindingResult binding, RedirectAttributes attr) {
		UserEntity user = this.userDAO.getCurrentUser();

		if (user == null) {
			return "redirect:/404";
		}

		if (binding.hasErrors()) {
			attr.addFlashAttribute("org.springframework.validation.BindingResult.userPasswordForm", binding);
			attr.addFlashAttribute("userPasswordForm", userPasswordForm);
			return "redirect:/users/change-password";
		}

		userPasswordForm.setPassword(this.passwordEncoder.encode(userPasswordForm.getPassword()));
		this.userDAO.update(user, userPasswordForm);

		return "redirect:/users";
	}

}
