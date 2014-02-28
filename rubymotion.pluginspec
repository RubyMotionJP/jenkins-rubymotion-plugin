Jenkins::Plugin::Specification.new do |plugin|
  plugin.name = "rubymotion"
  plugin.display_name = "Jenkins RubyMotion Plugin"
  plugin.version = '0.14'
  plugin.description = 'This plugin integrates RubyMotion with Jenkins.'

  # You should create a wiki-page for your plugin when you publish it, see
  # https://wiki.jenkins-ci.org/display/JENKINS/Hosting+Plugins#HostingPlugins-AddingaWikipage
  # This line makes sure it's listed in your POM.
  plugin.url = 'https://wiki.jenkins-ci.org/display/JENKINS/Jenkins+RubyMotion+Plugin'

  # The first argument is your user name for jenkins-ci.org.
  plugin.developed_by "watson1978", "Watson <watson1978@gmail.com>"

  # This specifies where your code is hosted.
  # Alternatives include:
  #  :github => 'myuser/rubymotion-plugin' (without myuser it defaults to jenkinsci)
  #  :git => 'git://repo.or.cz/rubymotion-plugin.git'
  #  :svn => 'https://svn.jenkins-ci.org/trunk/hudson/plugins/rubymotion-plugin'
  plugin.uses_repository :github => "Watson1978/jenkins-rubymotion-plugin"

  # This is a required dependency for every ruby plugin.
  plugin.depends_on 'ruby-runtime', '0.10'

  # This is a sample dependency for a Jenkins plugin, 'git'.
  #plugin.depends_on 'git', '1.1.11'
end
