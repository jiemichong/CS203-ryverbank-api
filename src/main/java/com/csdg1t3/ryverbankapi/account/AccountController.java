package com.csdg1t3.ryverbankapi.account;

import java.util.*;
import javax.validation.Valid;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// import jdk.jfr.internal.Cutoff;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import com.csdg1t3.ryverbankapi.user.*;

/**
 * Controller that manages HTTP GET/POST/PUT/DELETE requests by calling methods in AccountService
 */
@RestController
public class AccountController {
    private AccountService accountService;
    private UserService userService;
    private UserRepository customerRepo;

    public AccountController(AccountService accountService, UserRepository customerRepo, UserService userService) {
        this.accountService = accountService;
        this.customerRepo = customerRepo;
        this.userService = userService;
    }

    @GetMapping("/accounts")
    public List<Account> getAccounts() {
        return accountService.listAccounts();
    }

    @GetMapping("customer/{id}/accounts")
    public List<Account> getAccounts(@PathVariable Long id) {
        User userAtId = userService.getUser(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getPrincipal().toString().substring(0, auth.getPrincipal().toString().indexOf(" "));
        if(!userAtId.getUsername().equals(username)){
            throw new AccountNotValidException("User cannot view other user's account");
        }
        return accountService.listAccountsForUser(id);
    }

    // @PreAuthorize("#id == authentication.principal.id")
    @GetMapping("/accounts/{id}")
    public Account getAccount(@PathVariable Long id) {
        Account account = accountService.getAccount(id);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }

    @GetMapping("customer/{id}/accounts/{account_id}")
    public Account getAccount(@PathVariable Long id, @PathVariable Long account_id) {
        Account account = accountService.getAccount(account_id);
        if (account == null) {
            throw new AccountNotFoundException(account_id);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getPrincipal().toString().substring(0, auth.getPrincipal().toString().indexOf(" "));
        if(!account.getCustomer().getUsername().equals(username)){
            throw new AccountNotValidException("User cannot view other user's account");
        }
        
        if (account.getCustomerId() == id) {
            return account;
        } else {
            throw new AccountNotFoundException(account_id);
        }
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/accounts")
    public Account addAccount(@Valid @RequestBody Account account) {
        if(account.getBalance() < 5000.0 || account.getBalance().compareTo(account.getAvailableBalance()) != 0){
            throw new AccountNotValidException("Initial account balance must be more than 5000 and initial balance must match available balance");
        }
        Optional<User> result = customerRepo.findById(account.getCustomerId());
        if (result.isPresent()) {
            account.setCustomer(result.get());
            if (account.getCustomer().getStringAuthorities().contains("ROLE_MANAGER") 
                    || account.getCustomer().getStringAuthorities().contains("ROLE_ANALYST")) {
                throw new AccountNotValidException("Manager and Analyst cannot create account");
            }
            return accountService.addAccount(account);
        }
        throw new UserNotFoundException(account.getCustomerId());
    }

    @PutMapping("/accounts/{id}")
    public Account updateAccount(@PathVariable Long id, @Valid @RequestBody Account account) {
        account.setId(id);
        Account result = accountService.getAccount(id);
        
        if (result == null) {
            throw new AccountNotFoundException(id);
        }
        return accountService.updateAccount(id, account);
    }

    @DeleteMapping("/accounts/{id}")
    public void deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
        } catch (Exception e) {
            throw new AccountNotFoundException(id);
        }
    }
}