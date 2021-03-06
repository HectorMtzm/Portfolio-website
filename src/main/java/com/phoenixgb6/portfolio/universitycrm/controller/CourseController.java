package com.phoenixgb6.portfolio.universitycrm.controller;

import com.phoenixgb6.portfolio.universitycrm.entity.Course;
import com.phoenixgb6.portfolio.universitycrm.entity.Instructor;
import com.phoenixgb6.portfolio.universitycrm.entity.Review;
import com.phoenixgb6.portfolio.universitycrm.entity.Student;
import com.phoenixgb6.portfolio.universitycrm.exception.BadRequestException;
import com.phoenixgb6.portfolio.universitycrm.exception.NotFoundException;
import com.phoenixgb6.portfolio.universitycrm.service.ServiceS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("portfolio/universitycrm/courses")
public class CourseController {
    ServiceS<Course> courseService;
    ServiceS<Instructor> instructorService;
    ServiceS<Student> studentService;
    ServiceS<Review> reviewService;

    @Autowired
    public CourseController(ServiceS<Course> courseService, ServiceS<Instructor> instructorService,
                            ServiceS<Student> studentService, ServiceS<Review> reviewService) {
        this.courseService = courseService;
        this.instructorService = instructorService;
        this.studentService = studentService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public String coursesList(Model model,
                              @RequestParam(name = "page", required = false, defaultValue = "1") int pageNumber,
                              @RequestParam(name = "paSi", required = false, defaultValue = "15") int pageSize,
                              @RequestParam(name = "order", required = false, defaultValue = "1") int order,
                              @RequestParam(name = "search", required = false, defaultValue = "") String search){

        long count;

        if(search.equals("")){ count = courseService.count(); }
        else{ count = courseService.count(search); }

        int totalPages = (int) Math.floor(count / pageSize);

        if ((count % pageSize) > 0) {
            totalPages++;
        }

        List<Course> coursesList;

        try{
            coursesList = courseService.findAll(pageNumber, pageSize, order, search);
        }
        catch (Exception ex){
            model.addAttribute("project", "universitycrm");
            model.addAttribute("type", 'c');
            throw new BadRequestException("Your browser sent a request that this server could not understand", model);
        }

        model.addAttribute("paSi", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", count);
        model.addAttribute("page", pageNumber); //current page
        model.addAttribute("courses", coursesList);
        model.addAttribute("order", order);
        model.addAttribute("search", search);

        return "universitycrm/course-table";
    }

    @GetMapping("/{id}")
    public String getCourse(Model model, @PathVariable("id") int id){

        Course course;
        try{
            course = courseService.findById(id);
        }
        catch (Exception ex){
            model.addAttribute("project", "universitycrm");
            model.addAttribute("type", 'c');
            throw new NotFoundException("Course ID not found  -  " + id, model);
        }

        model.addAttribute("course", course);
        model.addAttribute("review", new Review());
        model.addAttribute("sid",null);

        return "universitycrm/course-info";
    }

    @PostMapping("/save")
    public String saveCourse(@ModelAttribute("course") @Valid Course course, BindingResult bindingResult,
                               @RequestParam("iid") int instructorId, Model model) {

        if(bindingResult.hasErrors()) {
            List<Instructor> instructorList = instructorService.findAll();

            model.addAttribute("instructorList", instructorList);

            return "universitycrm/course-form";
        }
        else{
            if(instructorId == 0){
                course.setInstructor(null);
            }
            else {
                course.setInstructor(instructorService.findById(instructorId));
            }

            // save the course
            courseService.save(course);

            // use a redirect to prevent duplicate submissions
            return "redirect:/portfolio/universitycrm/courses/";
        }
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("cid") int id, Model model) {

        // delete the course
        try{
            courseService.deleteById(id);
        }
        catch (Exception ex){
            model.addAttribute("project", "universitycrm");
            model.addAttribute("type", 'c');
            throw new NotFoundException("Course ID not found  -  " + id, model);
        }

        // redirect to /courses/
        return "redirect:/portfolio/universitycrm/courses";
    }

    @GetMapping("/addForm")
    public String addForm(Model model) {

        // create model attribute to bind form data
        Course course = new Course();
        List<Instructor> instructorList = instructorService.findAll();

        model.addAttribute("course", course);
        model.addAttribute("instructorList", instructorList);
        model.addAttribute("iid", 0);

        return "universitycrm/course-form";
    }

    @GetMapping("/updateForm")
    public String updateForm(@RequestParam("cid") int id, Model model, Integer instructorId) {

        Course course = courseService.findById(id);
        List<Instructor> instructorList = instructorService.findAll();

        Instructor instructor = course.getInstructor();
        if(instructor != null){
            instructorId = course.getInstructor().getId();
        }

        model.addAttribute("course", course);
        model.addAttribute("instructorList", instructorList);
        model.addAttribute("iid", instructorId);

        // send over to our form
        return "universitycrm/course-form";
    }

    @PostMapping("/{courseId}/saveReview")
    public String saveReview(@ModelAttribute("review") Review review, @PathVariable int courseId){

        Course course = courseService.findById(courseId);
        course.addReview(review);
        courseService.save(course);

        return "redirect:/portfolio/universitycrm/courses/" + course.getId();
    }

    @PostMapping("/{studentId}/saveStudent")
    public String saveStudent(@RequestParam("sid") int studentId, @RequestParam("cid") int courseId, Model model){

        Course course = courseService.findById(courseId);

        try{
            course.addStudent(studentService.findById(studentId));
            courseService.save(course);
        }
        catch (Exception ex){
            model.addAttribute("project", "universitycrm");
            model.addAttribute("type", 'c');
            throw new NotFoundException("Student ID not found  -  " + studentId, model);
        }


        return "redirect:/portfolio/universitycrm/courses/" + courseId;
    }

    @GetMapping("/deleteStudent")
    public String deleteStudent(@RequestParam("sid") int studentId, @RequestParam("cid") int courseId, Model model){

        Course course = courseService.findById(courseId);

        try{
            course.removeStudent(studentService.findById(studentId));
            courseService.save(course);
        }
        catch (Exception ex){
            model.addAttribute("project", "universitycrm");
            model.addAttribute("type", 'c');
            throw new NotFoundException("Student or course not found", model);
        }

        return "redirect:/portfolio/universitycrm/courses/" + courseId;
    }
}