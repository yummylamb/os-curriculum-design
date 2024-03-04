package com.os.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: lj
 * @desc
 * @create: 2024.03.03
 **/
@RequestMapping("/file")
@RestController
public class FileController {
    /**
     *
     * 若一个类中需要使用另一个类的功能，只需要在类中加入另一个类的service实现即可
     * 例：当前类中需要一个与内存有关的功能，需要如下操作
     * @Autowired
     * private StorageService storageService;
     */
}
