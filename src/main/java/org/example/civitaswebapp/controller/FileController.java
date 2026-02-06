//package org.example.civitaswebapp.controller;
//
//import org.example.civitaswebapp.domain.Invoice;
//import org.example.civitaswebapp.domain.MyUser;
//import org.example.civitaswebapp.repository.InvoiceRepository;
//import org.example.civitaswebapp.service.UserService;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.UrlResource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.net.MalformedURLException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Optional;

//@RestController
//@RequestMapping("/api/files")
//public class FileController {
//
//    private final InvoiceRepository invoiceRepository;
//    private final UserService userService;
//
//    public FileController(InvoiceRepository invoiceRepository, UserService userService) {
//        this.invoiceRepository = invoiceRepository;
//        this.userService = userService;
//    }
//
//    @GetMapping("/invoices/{filename}")
//    public ResponseEntity<Resource> downloadInvoice(@PathVariable String filename,
//                                                    @AuthenticationPrincipal MyUser currentUser) { // Spring injects the logged-in user

        // 1. SECURITY CHECK (The "Bouncer") 🛡️
        // In a real SaaS, we check: Does this file belong to the user's Union?
        // For now, let's just ensure the user is an ADMIN.
        // Later: Invoice inv = invoiceRepo.findByFilename(filename);
        //        if (!inv.getUnion().equals(currentUser.getUnion())) throw new AccessDeniedException();

        // 2. Locate the file on the disk (Server Side)
//        try {
//            Path filePath = Paths.get("./uploads/invoices").resolve(filename).normalize();
//            Resource resource = new UrlResource(filePath.toUri());
//
//            if (resource.exists()) {
//                // 3. Serve the file (Stream it)
//                return ResponseEntity.ok()
//                        .contentType(MediaType.APPLICATION_PDF)
//                        // "inline" means "show in browser", "attachment" means "force download"
//                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
//                        .body(resource);
//            } else {
//                return ResponseEntity.notFound().build();
//            }
//        } catch (MalformedURLException e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//}